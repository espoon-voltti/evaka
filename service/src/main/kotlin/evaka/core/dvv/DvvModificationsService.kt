// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import evaka.core.identity.Ssn
import evaka.core.pis.addSSNToPerson
import evaka.core.pis.applyDeathEffects
import evaka.core.pis.getPersonBySSN
import evaka.core.pis.getPersonIdsBySsns
import evaka.core.pis.lockPersonBySSN
import evaka.core.pis.replaceAddressPeriods
import evaka.core.pis.updatePersonFromVtj
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class DvvModificationsService(
    private val dvvModificationsServiceClient: DvvModificationsServiceClient,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {

    fun updatePersonsFromDvv(db: Database.Connection, clock: EvakaClock, ssns: List<String>): Int {
        var token = db.read { it.getNextDvvModificationToken() } ?: seedToken(db, clock)
        var modificationCount = 0
        while (true) {
            logger.debug {
                "Fetching dvv modifications with $token, found modifications so far: $modificationCount"
            }
            val page = dvvModificationsServiceClient.getModifications(token, ssns)
            processModifications(db, clock.now(), token, page, ssns.size)
            modificationCount += page.muutokset.size

            if (page.ajanTasalla) return modificationCount
            if (page.viimeisinKirjausavain == token) {
                error("DVV returned the same token $token for a page that is not up to date")
            }
            token = page.viimeisinKirjausavain
        }
    }

    private fun seedToken(db: Database.Connection, clock: EvakaClock): String {
        val today = clock.today()
        val firstToken =
            dvvModificationsServiceClient
                .getFirstModificationToken(today)
                ?.latestModificationToken
                ?.toString() ?: error("Could not fetch the first DVV modification token of $today")
        logger.info { "Seeding the DVV modification token from $today: $firstToken" }
        db.transaction { tx -> tx.storeDvvModificationToken("seed-$today", firstToken, 0, 0) }
        return firstToken
    }

    private fun processModifications(
        db: Database.Connection,
        now: HelsinkiDateTime,
        token: String,
        page: DvvModificationsResponse,
        ssnCount: Int,
    ) {
        val personIds = db.read { tx ->
            tx.getPersonIdsBySsns(
                    page.muutokset.mapNotNull { Ssn.parse(it.henkilotunnus) }.distinct()
                )
                .found
                .mapKeys { it.key.value }
        }
        logModifications(personIds, page.muutokset)

        val personsToUpdateFromVtj: MutableSet<PersonId> = mutableSetOf()

        page.muutokset.forEach { personModifications ->
            val personId = personIds[personModifications.henkilotunnus]
            personModifications.tietoryhmat.forEach { infoGroup ->
                try {
                    when (infoGroup) {
                        is DeathDvvInfoGroup -> {
                            handleDeath(db, now, personModifications.henkilotunnus, infoGroup)
                        }

                        is RestrictedInfoDvvInfoGroup -> {
                            handleRestrictedInfo(
                                db,
                                now,
                                personModifications.henkilotunnus,
                                infoGroup,
                            )
                        }

                        is SsnDvvInfoGroup -> {
                            handleSsnDvvInfoGroup(db, personModifications.henkilotunnus, infoGroup)
                        }

                        is CaretakerLimitedDvvInfoGroup,
                        is DefaultDvvInfoGroup -> {
                            personId?.let { personsToUpdateFromVtj.add(it) }
                        }

                        else -> {
                            logger.error {
                                "Refreshing person from VTJ for an unknown DVV modification type: ${infoGroup.tietoryhma} (all modification in this group: ${
                                    personModifications.tietoryhmat.joinToString(", ") { it.tietoryhma }
                                })"
                            }
                            personId?.let { personsToUpdateFromVtj.add(it) }
                        }
                    }
                } catch (e: Throwable) {
                    logger.error(e) {
                        "Could not process dvv modification for ${
                            personModifications.henkilotunnus.substring(
                                0,
                                6,
                            )
                        }: ${e.message}"
                    }
                    throw e
                }
            }
        }

        logger.info { "Dvv modifications: updating persons from VTJ: $personsToUpdateFromVtj" }

        db.transaction { tx ->
            // Read the SSNs after the loop: an SSN correction on this page may have changed them,
            // and the job finds the person by SSN
            asyncJobRunner.plan(
                tx,
                payloads =
                    tx.getSsnsOfPersons(personsToUpdateFromVtj).map { AsyncJob.UpdateFromVtj(it) },
                runAt = now,
            )
            if (token != page.viimeisinKirjausavain) {
                tx.storeDvvModificationToken(
                    token,
                    page.viimeisinKirjausavain,
                    ssnCount,
                    page.muutokset.size,
                )
            }
        }
    }

    private fun logModifications(
        personIds: Map<String, PersonId>,
        modifications: List<DvvModification>,
    ) {
        modifications.forEach { modification ->
            val person = personIds[modification.henkilotunnus]?.toString() ?: "not in eVaka"
            logger.info {
                "Dvv modification for $person at ${modification.muutospv}: ${
                    modification.tietoryhmat.joinToString(", ") { it.describe() }
                }"
            }
        }
    }

    private fun DvvInfoGroup.describe(): String {
        val dates =
            when (this) {
                is DefaultDvvInfoGroup -> {
                    listOf(
                        "alkupv" to alkupv,
                        "loppupv" to loppupv,
                        "huoltosuhteenAlkupv" to huoltosuhteenAlkupv,
                        "huoltosuhteenLoppupv" to huoltosuhteenLoppupv,
                    )
                }

                is CaretakerLimitedDvvInfoGroup -> {
                    listOf(
                        "huoltosuhteenAlkupv" to huoltosuhteenAlkupv,
                        "huoltosuhteenLoppupv" to huoltosuhteenLoppupv,
                    )
                }

                is DeathDvvInfoGroup -> {
                    listOf("kuolinpv" to kuolinpv)
                }

                is RestrictedInfoDvvInfoGroup -> {
                    listOf("turvaLoppuPv" to turvaLoppuPv)
                }

                else -> {
                    emptyList()
                }
            }
        return (listOf(tietoryhma, muutosattribuutti ?: "-") +
                dates.mapNotNull { (name, date) -> date?.let { "$name=${it.arvo}" } })
            .joinToString(" ", prefix = "[", postfix = "]")
    }

    private fun handleDeath(
        db: Database.Connection,
        now: HelsinkiDateTime,
        ssn: String,
        deathDvvInfoGroup: DeathDvvInfoGroup,
    ) {
        if (deathDvvInfoGroup.kuollut != true || deathDvvInfoGroup.kuolinpv == null) return

        db.transaction { tx ->
            tx.lockPersonBySSN(ssn)?.let { person ->
                val dateOfDeath = deathDvvInfoGroup.kuolinpv.asLocalDate()
                logger.info { "Dvv modification for ${person.id}: marking dead since $dateOfDeath" }
                tx.updatePersonFromVtj(person.copy(dateOfDeath = dateOfDeath), now)
                if (person.dateOfDeath == null) {
                    tx.applyDeathEffects(asyncJobRunner, now, person.id, dateOfDeath)
                }
            }
        }
    }

    private fun handleRestrictedInfo(
        db: Database.Connection,
        now: HelsinkiDateTime,
        ssn: String,
        restrictedInfoDvvInfoGroup: RestrictedInfoDvvInfoGroup,
    ) {
        val _ = db.transaction { tx ->
            tx.getPersonBySSN(ssn)?.let {
                logger.info {
                    "Dvv modification for ${it.id}: restricted ${restrictedInfoDvvInfoGroup.turvakieltoAktiivinen}"
                }
                tx.updatePersonFromVtj(
                    it.copy(
                        restrictedDetailsEnabled = restrictedInfoDvvInfoGroup.turvakieltoAktiivinen,
                        restrictedDetailsEndDate =
                            restrictedInfoDvvInfoGroup.turvaLoppuPv?.asLocalDate(),
                        streetAddress =
                            if (restrictedInfoDvvInfoGroup.turvakieltoAktiivinen) ""
                            else it.streetAddress,
                        postalCode =
                            if (restrictedInfoDvvInfoGroup.turvakieltoAktiivinen) ""
                            else it.postalCode,
                        postOffice =
                            if (restrictedInfoDvvInfoGroup.turvakieltoAktiivinen) ""
                            else it.postOffice,
                        mailingStreetAddress =
                            if (restrictedInfoDvvInfoGroup.turvakieltoAktiivinen) ""
                            else it.mailingStreetAddress,
                        mailingPostalCode =
                            if (restrictedInfoDvvInfoGroup.turvakieltoAktiivinen) ""
                            else it.mailingPostalCode,
                        mailingPostOffice =
                            if (restrictedInfoDvvInfoGroup.turvakieltoAktiivinen) ""
                            else it.mailingPostOffice,
                        municipalityOfResidence =
                            if (restrictedInfoDvvInfoGroup.turvakieltoAktiivinen) ""
                            else it.municipalityOfResidence,
                    ),
                    now,
                )
                if (restrictedInfoDvvInfoGroup.turvakieltoAktiivinen) {
                    tx.replaceAddressPeriods(it.id, emptyList())
                }
            }
        }
    }

    private fun handleSsnDvvInfoGroup(
        db: Database.Connection,
        ssn: String,
        ssnDvvInfoGroup: SsnDvvInfoGroup,
    ) {
        val _ = db.transaction { tx ->
            tx.getPersonBySSN(ssn)?.let {
                logger.info { "Dvv modification for ${it.id}: ssn change" }

                val newSsn = ssnDvvInfoGroup.aktiivinenHenkilotunnus?.let { ssn -> Ssn.parse(ssn) }
                if (newSsn != null) {
                    tx.addSSNToPerson(it.id, newSsn)
                } else {
                    logger.error { "Dvv modification for ${it.id}: ssn is missing or invalid" }
                }
            }
        }
    }
}
