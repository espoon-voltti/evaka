// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import fi.espoo.evaka.pis.Modifier
import fi.espoo.evaka.pis.ModifySource
import fi.espoo.evaka.pis.addSSNToPerson
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPartnersForPerson
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.updateParentshipDuration
import fi.espoo.evaka.pis.updatePartnershipDuration
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class DvvModificationsService(
    private val dvvModificationsServiceClient: DvvModificationsServiceClient,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>
) {

    fun updatePersonsFromDvv(db: Database.Connection, clock: EvakaClock, ssns: List<String>): Int {
        val result =
            db.transaction { getDvvModifications(it, ssns) }
                .let { modificationsForPersons ->
                    val ssnsToUpdateFromVtj: MutableSet<String> = emptySet<String>().toMutableSet()

                    modificationsForPersons.dvvModifications.map { personModifications ->
                        personModifications.tietoryhmat.map { infoGroup ->
                            try {
                                when (infoGroup) {
                                    is DeathDvvInfoGroup ->
                                        handleDeath(
                                            db,
                                            clock,
                                            personModifications.henkilotunnus,
                                            infoGroup
                                        )
                                    is RestrictedInfoDvvInfoGroup ->
                                        handleRestrictedInfo(
                                            db,
                                            personModifications.henkilotunnus,
                                            infoGroup
                                        )
                                    is SsnDvvInfoGroup ->
                                        handleSsnDvvInfoGroup(
                                            db,
                                            personModifications.henkilotunnus,
                                            infoGroup
                                        )
                                    is CaretakerLimitedDvvInfoGroup -> {
                                        if (infoGroup.huoltaja.henkilotunnus != null) {
                                            ssnsToUpdateFromVtj.add(
                                                infoGroup.huoltaja.henkilotunnus
                                            )
                                        } else {
                                            logger.info(
                                                "Dvv modification ignored for caretaker: ssn is null"
                                            )
                                        }
                                    }
                                    is DefaultDvvInfoGroup ->
                                        ssnsToUpdateFromVtj.add(personModifications.henkilotunnus)
                                    else -> {
                                        logger.error(
                                            "Refreshing person from VTJ for an unknown DVV modification type: ${infoGroup.tietoryhma} (all modification in this group: ${personModifications.tietoryhmat.map { it.tietoryhma }.joinToString(", ")})"
                                        )
                                        ssnsToUpdateFromVtj.add(personModifications.henkilotunnus)
                                    }
                                }
                            } catch (e: Throwable) {
                                logger.error(e) {
                                    "Could not process dvv modification for ${
                            personModifications.henkilotunnus.substring(
                                0,
                                6
                            )
                            }: ${e.message}"
                                }
                                throw e
                            }
                        }
                    }

                    logger.info(
                        "Dvv modifications: updating ${ssnsToUpdateFromVtj.size} persons from VTJ"
                    )

                    db.transaction { tx ->
                        asyncJobRunner.plan(
                            tx,
                            payloads = ssnsToUpdateFromVtj.map { AsyncJob.UpdateFromVtj(it) },
                            runAt = clock.now()
                        )
                    }

                    modificationsForPersons
                }

        if (result.token != result.nextToken) {
            db.transaction {
                it.storeDvvModificationToken(
                    result.token,
                    result.nextToken,
                    ssns.size,
                    result.dvvModifications.size
                )
            }
        }

        return result.dvvModifications.size
    }

    private fun handleDeath(
        db: Database.Connection,
        clock: EvakaClock,
        ssn: String,
        deathDvvInfoGroup: DeathDvvInfoGroup
    ) {
        if (deathDvvInfoGroup.kuollut != true || deathDvvInfoGroup.kuolinpv == null) return

        db.transaction { tx ->
            tx.getPersonBySSN(ssn)?.let { person ->
                val dateOfDeath = deathDvvInfoGroup.kuolinpv.asLocalDate()
                logger.info("Dvv modification for ${person.id}: marking dead since $dateOfDeath")
                tx.updatePersonFromVtj(person.copy(dateOfDeath = dateOfDeath))

                endFamilyRelations(tx, person.id, dateOfDeath, clock)
                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            person.id,
                            DateRange(dateOfDeath, null)
                        )
                    ),
                    runAt = clock.now()
                )
                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forChild(
                            person.id,
                            DateRange(dateOfDeath, null)
                        )
                    ),
                    runAt = clock.now()
                )
            }
        }
    }

    private fun endFamilyRelations(
        tx: Database.Transaction,
        personId: PersonId,
        dateOfDeath: LocalDate,
        clock: EvakaClock
    ) {
        tx.getPartnersForPerson(
                personId,
                includeConflicts = true,
                period = DateRange(dateOfDeath, dateOfDeath)
            )
            .forEach {
                tx.updatePartnershipDuration(
                    it.partnershipId,
                    it.startDate,
                    dateOfDeath,
                    ModifySource.DVV,
                    clock.now(),
                    null
                )
            }

        val parentships =
            tx.getParentships(
                headOfChildId = personId,
                childId = null,
                includeConflicts = true,
                period = DateRange(dateOfDeath, dateOfDeath)
            ) +
                tx.getParentships(
                    headOfChildId = null,
                    childId = personId,
                    includeConflicts = true,
                    period = DateRange(dateOfDeath, dateOfDeath)
                )

        parentships.forEach {
            tx.updateParentshipDuration(
                id = it.id,
                startDate = it.startDate,
                endDate = dateOfDeath,
                now = clock.now(),
                modifier = Modifier.DVV
            )
        }
    }

    private fun handleRestrictedInfo(
        db: Database.Connection,
        ssn: String,
        restrictedInfoDvvInfoGroup: RestrictedInfoDvvInfoGroup
    ) =
        db.transaction { tx ->
            tx.getPersonBySSN(ssn)?.let {
                logger.info(
                    "Dvv modification for ${it.id}: restricted ${restrictedInfoDvvInfoGroup.turvakieltoAktiivinen}"
                )
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
                            else it.postOffice
                    )
                )
            }
        }

    private fun handleSsnDvvInfoGroup(
        db: Database.Connection,
        ssn: String,
        ssnDvvInfoGroup: SsnDvvInfoGroup
    ) =
        db.transaction { tx ->
            tx.getPersonBySSN(ssn)?.let {
                logger.info("Dvv modification for ${it.id}: ssn change")

                if (!ssnDvvInfoGroup.aktiivinenHenkilotunnus.isNullOrEmpty()) {
                    tx.addSSNToPerson(it.id, ssnDvvInfoGroup.aktiivinenHenkilotunnus)
                } else {
                    logger.error("Dvv modification for ${it.id}: ssn is set to null or empty")
                }
            }
        }

    data class DvvModificationsWithToken(
        val dvvModifications: List<DvvModification>,
        val token: String,
        val nextToken: String
    )

    fun getDvvModifications(tx: Database.Read, ssns: List<String>): DvvModificationsWithToken {
        val token = tx.getNextDvvModificationToken()
        return getAllPagesOfDvvModifications(ssns, token, emptyList())
    }

    fun getAllPagesOfDvvModifications(
        ssns: List<String>,
        token: String,
        alreadyFoundDvvModifications: List<DvvModification>
    ): DvvModificationsWithToken {
        logger.debug(
            "Fetching dvv modifications with $token, found modifications so far: ${alreadyFoundDvvModifications.size}"
        )
        return dvvModificationsServiceClient.getModifications(token, ssns).let {
            dvvModificationsResponse ->
            val combinedModifications =
                alreadyFoundDvvModifications + dvvModificationsResponse.muutokset
            if (dvvModificationsResponse.ajanTasalla) {
                DvvModificationsWithToken(
                    dvvModifications = combinedModifications,
                    token = token,
                    nextToken = dvvModificationsResponse.viimeisinKirjausavain
                )
            } else {
                getAllPagesOfDvvModifications(
                    ssns,
                    dvvModificationsResponse.viimeisinKirjausavain,
                    combinedModifications
                )
            }
        }
    }
}
