// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.dvv

import fi.espoo.evaka.pis.addSSNToPerson
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

class DvvModificationsService(
    private val jdbi: Jdbi,
    private val dvvModificationsServiceClient: DvvModificationsServiceClient
) {

    fun updatePersonsFromDvv(ssns: List<String>) {
        getDvvModifications(ssns).map { personDvvModifications ->
            personDvvModifications.infoGroups.map { infoGroup ->
                when (infoGroup) {
                    is DeathDvvInfoGroup -> handleDeath(personDvvModifications.ssn, infoGroup)
                    is RestrictedInfoDvvInfoGroup -> handleRestrictedInfo(personDvvModifications.ssn, infoGroup)
                    is SsnDvvInfoGroup -> handleSsnDvvInfoGroup(personDvvModifications.ssn, infoGroup)
                    is AddressDvvInfoGroup -> handleAddressDvvInfoGroup(personDvvModifications.ssn, infoGroup)
                    // is CustodianLimitedDvvInfoGroup -> handleCustodianLimitedDvvInfoGroup(personDvvModifications.ssn, infoGroup)
                    // is CaretakerLimitedDvvInfoGroup -> handleCaretakerLimitedDvvInfoGroup(personDvvModifications.ssn, infoGroup)
                    // is PersonNameDvvInfoGroup -> handlePersonNameDvvInfoGroup(personDvvModifications.ssn, infoGroup)
                    // is PersonNameChangeDvvInfoGroup -> handlePersonNameChangeDvvInfoGroup(personDvvModifications.ssn, infoGroup)
                    else -> logger.info("Unsupported DVV modification: ${infoGroup.type}")
                }
            }
        }

        // Todo: flush VTJ cache
    }

    private fun handleDeath(ssn: String, deathDvvInfoGroup: DeathDvvInfoGroup) {
        jdbi.handle { h ->
            h.getPersonBySSN(ssn)?.let {
                val dateOfDeath = deathDvvInfoGroup.dateOfDeath?.asLocalDate() ?: LocalDate.now()
                logger.debug("Dvv modification for ${it.id}: marking dead since $dateOfDeath")
                h.updatePersonFromVtj(it.copy(dateOfDeath = dateOfDeath))
            }
        }
    }

    private fun handleRestrictedInfo(ssn: String, restrictedInfoDvvInfoGroup: RestrictedInfoDvvInfoGroup) {
        jdbi.handle { h ->
            h.getPersonBySSN(ssn)?.let {
                logger.debug("Dvv modification for ${it.id}: restricted ${restrictedInfoDvvInfoGroup.restrictedActive}")
                h.updatePersonFromVtj(
                    it.copy(
                        restrictedDetailsEnabled = restrictedInfoDvvInfoGroup.restrictedActive,
                        restrictedDetailsEndDate = restrictedInfoDvvInfoGroup.restrictedEndDate?.asLocalDate()
                    )
                )
            }
        }
    }

    private fun handleSsnDvvInfoGroup(ssn: String, ssnDvvInfoGroup: SsnDvvInfoGroup) {
        jdbi.handle { h ->
            h.getPersonBySSN(ssn)?.let {
                logger.debug("Dvv modification for ${it.id}: ssn change")
                h.addSSNToPerson(it.id, ssnDvvInfoGroup.activeSsn)
            }
        }
    }

    private fun handleAddressDvvInfoGroup(ssn: String, addressDvvInfoGroup: AddressDvvInfoGroup) {
        jdbi.handle { h ->
            h.getPersonBySSN(ssn)?.let {
                logger.debug("Dvv modification for ${it.id}: address change")
                h.updatePersonFromVtj(
                    it.copy(
                        streetAddress = addressDvvInfoGroup.streetAddress(),
                        postalCode = addressDvvInfoGroup.postalCode ?: "",
                        postOffice = addressDvvInfoGroup.postOffice?.fi ?: ""
                        // TODO: residence code
                    )
                )
            }
        }
    }

    /*
        private fun handleCustodianLimitedDvvInfoGroup(ssn: String, custodianLimitedDvvInfoGroup: CustodianLimitedDvvInfoGroup) {
        }

        private fun handleCaretakerLimitedDvvInfoGroup(ssn: String, caretakerLimitedDvvInfoGroup: CaretakerLimitedDvvInfoGroup) {
        }


        private fun handlePersonNameDvvInfoGroup(ssn: String, personNameDvvInfoGroup: PersonNameDvvInfoGroup) {
        }

        private fun handlePersonNameChangeDvvInfoGroup(ssn: String, personNameChangeDvvInfoGroup: PersonNameChangeDvvInfoGroup) {
        }
    */
    fun getDvvModifications(ssns: List<String>): List<DvvModification> = jdbi.transaction { h ->
        val token = getNextDvvModificationToken(h)
        dvvModificationsServiceClient.getModifications(token, ssns)?.let { dvvModificationsResponse ->
            storeDvvModificationToken(h, token, dvvModificationsResponse.modificationToken, ssns.size, dvvModificationsResponse.modifications.size)
            dvvModificationsResponse.modifications
        } ?: emptyList()
    }
}

// Forms a list of persons' ssn's that should be updated from DVV
fun getPersonSsnsToUpdate(jdbi: Jdbi): List<String> = jdbi.handle { h ->
    //language=sql
    h.createQuery(
        """
SELECT DISTINCT(social_security_number) from PERSON p JOIN (
SELECT head_of_child FROM fridge_child
WHERE daterange(start_date, end_date, '[]') @> current_date AND conflict = false) hoc ON p.id = hoc.head_of_child
        """.trimIndent()
    )
        .mapTo<String>()
        .toList()
}
