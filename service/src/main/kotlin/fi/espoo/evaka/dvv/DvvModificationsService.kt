// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.dvv

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.addSSNToPerson
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.service.FridgeFamilyService
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.shared.async.VTJRefresh
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Service
class DvvModificationsService(
    private val dvvModificationsServiceClient: DvvModificationsServiceClient,
    private val personService: PersonService,
    private val fridgeFamilyService: FridgeFamilyService
) {

    fun updatePersonsFromDvv(db: Database, ssns: List<String>): Int {
        return db.transaction { getDvvModifications(it, ssns) }.let { modificationsForPersons ->
            modificationsForPersons.map { personModifications ->
                personModifications.tietoryhmat.map { infoGroup ->
                    try {
                        when (infoGroup) {
                            is DeathDvvInfoGroup -> handleDeath(db, personModifications.henkilotunnus, infoGroup)
                            is RestrictedInfoDvvInfoGroup -> handleRestrictedInfo(
                                db,
                                personModifications.henkilotunnus,
                                infoGroup
                            )
                            is SsnDvvInfoGroup -> handleSsnDvvInfoGroup(
                                db,
                                personModifications.henkilotunnus,
                                infoGroup
                            )
                            is AddressDvvInfoGroup -> handleAddressDvvInfoGroup(
                                db,
                                personModifications.henkilotunnus,
                                infoGroup
                            )
                            is ResidenceCodeDvvInfoGroup -> handleResidenceCodeDvvInfoGroup(
                                db,
                                personModifications.henkilotunnus,
                                infoGroup
                            )
                            is CustodianLimitedDvvInfoGroup -> handleCustodianLimitedDvvInfoGroup(
                                db,
                                personModifications.henkilotunnus,
                                infoGroup
                            )
                            is CaretakerLimitedDvvInfoGroup -> handleCaretakerLimitedDvvInfoGroup(db, infoGroup)
                            is PersonNameDvvInfoGroup -> handlePersonNameDvvInfoGroup(
                                db,
                                personModifications.henkilotunnus,
                                infoGroup
                            )
                            is PersonNameChangeDvvInfoGroup -> handlePersonNameChangeDvvInfoGroup(
                                db,
                                personModifications.henkilotunnus,
                                infoGroup
                            )
                            is HomeMunicipalityDvvInfoGroup -> handleHomeMunicipalityChangeDvvInfoGroup()
                            else -> logger.info("Unsupported DVV modification: ${infoGroup.tietoryhma}")
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
                    }
                }
            }

            modificationsForPersons.size
        }
    }

    private fun handleDeath(db: Database, ssn: String, deathDvvInfoGroup: DeathDvvInfoGroup) = db.transaction { tx ->
        tx.handle.getPersonBySSN(ssn)?.let {
            val dateOfDeath = deathDvvInfoGroup.kuolinpv?.asLocalDate() ?: LocalDate.now()
            logger.info("Dvv modification for ${it.id}: marking dead since $dateOfDeath")
            tx.updatePersonFromVtj(it.copy(dateOfDeath = dateOfDeath))
        }
    }

    private fun handleRestrictedInfo(
        db: Database,
        ssn: String,
        restrictedInfoDvvInfoGroup: RestrictedInfoDvvInfoGroup
    ) = db.transaction { tx ->
        tx.handle.getPersonBySSN(ssn)?.let {
            logger.info("Dvv modification for ${it.id}: restricted ${restrictedInfoDvvInfoGroup.turvakieltoAktiivinen}")
            tx.updatePersonFromVtj(
                it.copy(
                    restrictedDetailsEnabled = restrictedInfoDvvInfoGroup.turvakieltoAktiivinen,
                    restrictedDetailsEndDate = restrictedInfoDvvInfoGroup.turvaLoppuPv?.asLocalDate(),
                    streetAddress = if (restrictedInfoDvvInfoGroup.turvakieltoAktiivinen) "" else it.streetAddress,
                    postalCode = if (restrictedInfoDvvInfoGroup.turvakieltoAktiivinen) "" else it.postalCode,
                    postOffice = if (restrictedInfoDvvInfoGroup.turvakieltoAktiivinen) "" else it.postOffice
                )
            )
        }
    }

    private fun handleSsnDvvInfoGroup(db: Database, ssn: String, ssnDvvInfoGroup: SsnDvvInfoGroup) =
        db.transaction { tx ->
            tx.handle.getPersonBySSN(ssn)?.let {
                logger.info("Dvv modification for ${it.id}: ssn change")
                tx.handle.addSSNToPerson(it.id, ssnDvvInfoGroup.aktiivinenHenkilotunnus)
            }
        }

    // We get records LISATTY + MUUTETTU if address has changed (LISATTY is the new address),
    // TURVAKIELTO=false and MUUTETTU if restrictions are lifted (MUUTETTU is the "new" address)
    private fun handleAddressDvvInfoGroup(db: Database, ssn: String, addressDvvInfoGroup: AddressDvvInfoGroup) =
        db.transaction { tx ->
            tx.handle.getPersonBySSN(ssn)?.let {
                if (addressDvvInfoGroup.muutosattribuutti.equals("LISATTY") || (
                    addressDvvInfoGroup.muutosattribuutti.equals("MUUTETTU") && it.streetAddress.isNullOrEmpty()
                    )
                ) {
                    logger.info("Dvv modification for ${it.id}: address change, type: ${addressDvvInfoGroup.muutosattribuutti}")
                    tx.updatePersonFromVtj(
                        it.copy(
                            streetAddress = addressDvvInfoGroup.katuosoite(),
                            postalCode = addressDvvInfoGroup.postinumero ?: "",
                            postOffice = addressDvvInfoGroup.postitoimipaikka?.fi ?: ""
                        )
                    )
                }
            }
        }

    private fun handleResidenceCodeDvvInfoGroup(
        db: Database,
        ssn: String,
        residenceCodeDvvInfoGroup: ResidenceCodeDvvInfoGroup
    ) = db.transaction { tx ->
        if (residenceCodeDvvInfoGroup.muutosattribuutti.equals("LISATTY")) {
            tx.handle.getPersonBySSN(ssn)?.let {
                logger.info("Dvv modification for ${it.id}: residence code change")
                tx.updatePersonFromVtj(
                    it.copy(
                        residenceCode = residenceCodeDvvInfoGroup.asuinpaikantunnus
                    )
                )
            }
        }
    }

    private fun handleCustodianLimitedDvvInfoGroup(
        db: Database,
        ssn: String,
        custodianLimitedDvvInfoGroup: CustodianLimitedDvvInfoGroup
    ) {
        val user = AuthenticatedUser.SystemInternalUser
        db.transaction { tx ->
            personService.getOrCreatePerson(tx, user, ExternalIdentifier.SSN.getInstance(ssn))
        }?.let {
            logger.info("Dvv modification ${custodianLimitedDvvInfoGroup.muutosattribuutti} for ${it.id}: refreshing all custodian/caretaker info from DVV")
            fridgeFamilyService.doVTJRefresh(db, VTJRefresh(it.id, user.id))
        }
    }

    private fun handleCaretakerLimitedDvvInfoGroup(
        db: Database,
        caretakerLimitedDvvInfoGroup: CaretakerLimitedDvvInfoGroup
    ) {
        val user = AuthenticatedUser.SystemInternalUser
        if (caretakerLimitedDvvInfoGroup.huoltaja.henkilotunnus != null) {
            db.transaction { tx ->
                personService.getOrCreatePerson(
                    tx,
                    user,
                    ExternalIdentifier.SSN.getInstance(caretakerLimitedDvvInfoGroup.huoltaja.henkilotunnus)
                )
            }?.let {
                logger.info("Dvv modification ${caretakerLimitedDvvInfoGroup.muutosattribuutti} for ${it.id}: refreshing all caretaker/custodian info from DVV")
                fridgeFamilyService.doVTJRefresh(db, VTJRefresh(it.id, user.id))
            }
        } else {
            logger.info("Dvv modification ignored for caretaker: ssn is null")
        }
    }

    private fun handlePersonNameDvvInfoGroup(
        db: Database,
        ssn: String,
        personNameDvvInfoGroup: PersonNameDvvInfoGroup
    ) = db.transaction { tx ->
        tx.handle.getPersonBySSN(ssn)?.let {
            val user = AuthenticatedUser.SystemInternalUser
            personService.getOrCreatePerson(tx, user, ExternalIdentifier.SSN.getInstance(ssn))?.let {
                logger.info("Dvv modification for ${it.id}: name ${personNameDvvInfoGroup.muutosattribuutti}, refreshed all info from DVV")
            }
        }
    }

    private fun handlePersonNameChangeDvvInfoGroup(
        db: Database,
        ssn: String,
        personNameChangeDvvInfoGroup: PersonNameChangeDvvInfoGroup
    ) = db.transaction { tx ->
        tx.handle.getPersonBySSN(ssn)?.let {
            val user = AuthenticatedUser.SystemInternalUser
            personService.getOrCreatePerson(tx, user, ExternalIdentifier.SSN.getInstance(ssn))?.let {
                logger.info("Dvv modification for ${it.id}: name has changed: ${personNameChangeDvvInfoGroup.muutosattribuutti} - ${personNameChangeDvvInfoGroup.nimilaji}, refreshed all info from DVV")
            }
        }
    }

    // KOTIKUNTA is received as part of the other address change info groups, the actual address change
    // is done in those
    private fun handleHomeMunicipalityChangeDvvInfoGroup() {
        logger.debug("DVV change KOTIKUNTA received")
    }

    fun getDvvModifications(tx: Database.Transaction, ssns: List<String>): List<DvvModification> {
        val token = getNextDvvModificationToken(tx.handle)
        return getAllPagesOfDvvModifications(tx, ssns, token, emptyList())
    }

    fun getAllPagesOfDvvModifications(
        tx: Database.Transaction,
        ssns: List<String>,
        token: String,
        alreadyFoundDvvModifications: List<DvvModification>
    ): List<DvvModification> {
        logger.debug("Fetching dvv modifications with $token, found modifications so far: ${alreadyFoundDvvModifications.size}")
        return dvvModificationsServiceClient.getModifications(token, ssns).let { dvvModificationsResponse ->
            val combinedModifications = alreadyFoundDvvModifications + dvvModificationsResponse.muutokset
            if (dvvModificationsResponse.ajanTasalla) {
                if (dvvModificationsResponse.viimeisinKirjausavain != token)
                    storeDvvModificationToken(
                        tx.handle,
                        token,
                        dvvModificationsResponse.viimeisinKirjausavain,
                        ssns.size,
                        dvvModificationsResponse.muutokset.size
                    )
                combinedModifications
            } else {
                getAllPagesOfDvvModifications(
                    tx,
                    ssns,
                    dvvModificationsResponse.viimeisinKirjausavain,
                    combinedModifications
                )
            }
        }
    }
}
