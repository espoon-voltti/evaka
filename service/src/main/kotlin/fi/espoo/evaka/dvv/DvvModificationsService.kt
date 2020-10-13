// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
package fi.espoo.evaka.dvv

import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.db.handle
import getNextDvvModificationToken
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.transaction.annotation.Transactional
import storeDvvModificationToken
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Transactional
class DvvModificationsService(
    private val jdbi: Jdbi,
    private val dvvModificationsServiceClient: DvvModificationsServiceClient,
    private val personService: PersonService

) {

    fun updatePersonsFromDvv(ssns: List<String>) {
        getDvvModifications(ssns).map { personDvvModifications ->
            personDvvModifications.infoGroups.map { infoGroup ->
                when (infoGroup) {
                    is DeathDvvInfoGroup -> handleDeath(personDvvModifications.ssn, infoGroup)
                    else -> logger.info("Unsupported DVV modification: ${infoGroup.type}")
                }
            }
        }
    }

    fun handleDeath(ssn: String, deathDvvInfoGroup: DeathDvvInfoGroup) {
        personService.getPersonBySsn(ssn)?.let {
            val dateOfDeath = deathDvvInfoGroup.dateOfDeath?.asLocalDate() ?: LocalDate.now()
            logger.debug("Dvv modification: marking ${it.id} dead since $dateOfDeath")
            jdbi.handle { h ->
                setPersonDateOfDeath(h, it.id, dateOfDeath)
            }
        }
    }

    fun getDvvModifications(ssns: List<String>): List<DvvModification> {
        val token = getNextDvvModificationToken(jdbi)
        return dvvModificationsServiceClient.getModifications(token, ssns)?.let { dvvModificationsResponse ->
            storeDvvModificationToken(jdbi, token, dvvModificationsResponse.modificationToken, ssns.size, dvvModificationsResponse.modifications.size)
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
