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
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.springframework.stereotype.Service
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Service
class DvvModificationsService(
    private val jdbi: Jdbi,
    private val dvvModificationsServiceClient: DvvModificationsServiceClient,
    private val personService: PersonService,
    private val fridgeFamilyService: FridgeFamilyService
) {

    fun updatePersonsFromDvv(ssns: List<String>): Int {
        return jdbi.handle { getDvvModifications(it, ssns) }.let { modificationsForPersons ->
            modificationsForPersons.map { personModifications ->
                personModifications.tietoryhmat.map { infoGroup ->
                    try {
                        when (infoGroup) {
                            is DeathDvvInfoGroup -> handleDeath(personModifications.henkilotunnus, infoGroup)
                            is RestrictedInfoDvvInfoGroup -> handleRestrictedInfo(personModifications.henkilotunnus, infoGroup)
                            is SsnDvvInfoGroup -> handleSsnDvvInfoGroup(personModifications.henkilotunnus, infoGroup)
                            is AddressDvvInfoGroup -> handleAddressDvvInfoGroup(personModifications.henkilotunnus, infoGroup)
                            is ResidenceCodeDvvInfoGroup -> handleResidenceCodeDvvInfoGroup(personModifications.henkilotunnus, infoGroup)
                            is CustodianLimitedDvvInfoGroup -> handleCustodianLimitedDvvInfoGroup(personModifications.henkilotunnus, infoGroup)
                            is CaretakerLimitedDvvInfoGroup -> handleCaretakerLimitedDvvInfoGroup(infoGroup)
                            is PersonNameDvvInfoGroup -> handlePersonNameDvvInfoGroup(personModifications.henkilotunnus, infoGroup)
                            is PersonNameChangeDvvInfoGroup -> handlePersonNameChangeDvvInfoGroup(personModifications.henkilotunnus, infoGroup)
                            else -> logger.info("Unsupported DVV modification: ${infoGroup.tietoryhma}")
                        }
                    } catch (e: Throwable) {
                        logger.error("Could not process dvv modification for ${personModifications.henkilotunnus.substring(0, 6)}: ${e.message}")
                    }
                }
            }

            logger.info("Processed ${modificationsForPersons.size} DVV person modifications")
            modificationsForPersons.size
        }
    }

    private fun handleDeath(ssn: String, deathDvvInfoGroup: DeathDvvInfoGroup) {
        jdbi.handle { h ->
            h.getPersonBySSN(ssn)?.let {
                val dateOfDeath = deathDvvInfoGroup.kuolinpv?.asLocalDate() ?: LocalDate.now()
                logger.debug("Dvv modification for ${it.id}: marking dead since $dateOfDeath")
                h.updatePersonFromVtj(it.copy(dateOfDeath = dateOfDeath))
            }
        }
    }

    private fun handleRestrictedInfo(ssn: String, restrictedInfoDvvInfoGroup: RestrictedInfoDvvInfoGroup) {
        jdbi.handle { h ->
            h.getPersonBySSN(ssn)?.let {
                logger.debug("Dvv modification for ${it.id}: restricted ${restrictedInfoDvvInfoGroup.turvakieltoAktiivinen}")
                h.updatePersonFromVtj(
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
    }

    private fun handleSsnDvvInfoGroup(ssn: String, ssnDvvInfoGroup: SsnDvvInfoGroup) {
        jdbi.handle { h ->
            h.getPersonBySSN(ssn)?.let {
                logger.debug("Dvv modification for ${it.id}: ssn change")
                h.addSSNToPerson(it.id, ssnDvvInfoGroup.aktiivinenHenkilotunnus)
            }
        }
    }

    // We get records LISATTY + MUUTETTU if address has changed (LISATTY is the new address),
    // TURVAKIELTO=false and MUUTETTU if restrictions are lifted (MUUTETTU is the "new" address)
    private fun handleAddressDvvInfoGroup(ssn: String, addressDvvInfoGroup: AddressDvvInfoGroup) {
        jdbi.handle { h ->
            h.getPersonBySSN(ssn)?.let {
                if (addressDvvInfoGroup.muutosattribuutti.equals("LISATTY") || (
                    addressDvvInfoGroup.muutosattribuutti.equals("MUUTETTU") && it.streetAddress.isNullOrEmpty()
                    )
                ) {
                    logger.debug("Dvv modification for ${it.id}: address change, type: ${addressDvvInfoGroup.muutosattribuutti}")
                    h.updatePersonFromVtj(
                        it.copy(
                            streetAddress = addressDvvInfoGroup.katuosoite(),
                            postalCode = addressDvvInfoGroup.postinumero ?: "",
                            postOffice = addressDvvInfoGroup.postitoimipaikka?.fi ?: ""
                        )
                    )
                }
            }
        }
    }

    private fun handleResidenceCodeDvvInfoGroup(ssn: String, residenceCodeDvvInfoGroup: ResidenceCodeDvvInfoGroup) {
        if (residenceCodeDvvInfoGroup.muutosattribuutti.equals("LISATTY")) {
            jdbi.handle { h ->
                h.getPersonBySSN(ssn)?.let {
                    logger.debug("Dvv modification for ${it.id}: residence code change")
                    h.updatePersonFromVtj(
                        it.copy(
                            residenceCode = residenceCodeDvvInfoGroup.asuinpaikantunnus
                        )
                    )
                }
            }
        }
    }

    private fun handleCustodianLimitedDvvInfoGroup(ssn: String, custodianLimitedDvvInfoGroup: CustodianLimitedDvvInfoGroup) {
        if (custodianLimitedDvvInfoGroup.muutosattribuutti == "LISATTY") {
            val user = AuthenticatedUser.anonymous
            personService.getOrCreatePerson(user, ExternalIdentifier.SSN.getInstance(ssn))?.let {
                logger.debug("Dvv modification for ${it.id}: has a new custodian, refreshing all info from DVV")
                jdbi.transaction { h -> fridgeFamilyService.doVTJRefresh(h, VTJRefresh(it.id, user.id)) }
            }
        }
    }

    private fun handleCaretakerLimitedDvvInfoGroup(caretakerLimitedDvvInfoGroup: CaretakerLimitedDvvInfoGroup) {
        if (caretakerLimitedDvvInfoGroup.muutosattribuutti == "LISATTY") {
            val user = AuthenticatedUser.anonymous
            personService.getOrCreatePerson(user, ExternalIdentifier.SSN.getInstance(caretakerLimitedDvvInfoGroup.huoltaja.henkilotunnus))?.let {
                logger.debug("Dvv modification for ${it.id}: a new caretaker added, refreshing all info from DVV")
                jdbi.transaction { h -> fridgeFamilyService.doVTJRefresh(h, VTJRefresh(it.id, user.id)) }
            }
        }
    }

    private fun handlePersonNameDvvInfoGroup(ssn: String, personNameDvvInfoGroup: PersonNameDvvInfoGroup) {
        jdbi.handle { h ->
            h.getPersonBySSN(ssn)?.let {
                val user = AuthenticatedUser.anonymous
                personService.getOrCreatePerson(user, ExternalIdentifier.SSN.getInstance(ssn))?.let {
                    logger.debug("Dvv modification for ${it.id}: name ${personNameDvvInfoGroup.muutosattribuutti}, refreshed all info from DVV")
                }
            }
        }
    }

    private fun handlePersonNameChangeDvvInfoGroup(ssn: String, personNameChangeDvvInfoGroup: PersonNameChangeDvvInfoGroup) {
        jdbi.handle { h ->
            h.getPersonBySSN(ssn)?.let {
                val user = AuthenticatedUser.anonymous
                personService.getOrCreatePerson(user, ExternalIdentifier.SSN.getInstance(ssn))?.let {
                    logger.debug("Dvv modification for ${it.id}: name has changed: ${personNameChangeDvvInfoGroup.muutosattribuutti} - ${personNameChangeDvvInfoGroup.nimilaji}, refreshed all info from DVV")
                }
            }
        }
    }

    fun getDvvModifications(h: Handle, ssns: List<String>): List<DvvModification> {
        val token = getNextDvvModificationToken(h)
        return getAllPagesOfDvvModifications(h, ssns, token, emptyList())
    }

    fun getAllPagesOfDvvModifications(h: Handle, ssns: List<String>, token: String, alreadyFoundDvvModifications: List<DvvModification>): List<DvvModification> {
        logger.debug("Fetching dvv modifications with $token, found modifications so far: ${alreadyFoundDvvModifications.size}")
        return dvvModificationsServiceClient.getModifications(token, ssns).let { dvvModificationsResponse ->
            val combinedModifications = alreadyFoundDvvModifications + dvvModificationsResponse.muutokset
            if (dvvModificationsResponse.ajanTasalla) {
                if (dvvModificationsResponse.viimeisinKirjausavain != token)
                    storeDvvModificationToken(h, token, dvvModificationsResponse.viimeisinKirjausavain, ssns.size, dvvModificationsResponse.muutokset.size)
                combinedModifications
            } else {
                getAllPagesOfDvvModifications(h, ssns, dvvModificationsResponse.viimeisinKirjausavain, combinedModifications)
            }
        }
    }
}
