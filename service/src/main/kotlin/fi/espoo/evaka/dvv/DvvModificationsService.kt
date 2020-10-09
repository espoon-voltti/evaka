package fi.espoo.evaka.dvv

import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DvvModificationsService(
        private val jdbi: Jdbi,
        private val dvvModificationsServiceClient: DvvModificationsServiceClient
) {

    private fun getDvvModifications(ssns: List<String>): DvvModificationsResponse? {
        return dvvModificationsServiceClient.getModifications(getLatestDvvModificationToken(), ssns)
    }

    private fun storeDvvModificationToken(token: String) = jdbi.transaction { h ->
        h.createUpdate("""
INSERT INTO dvv_modification_token (token) VALUES (:token)
        """.trimIndent()).bind("token", token).execute()
    }

    private fun getLatestDvvModificationToken(): String = jdbi.handle { h ->
        h.createQuery("""
SELECT token 
FROM dvv_modification_token
ORDER BY TIMESTAMP DESC
LIMIT 1
        """.trimIndent()).mapTo<String>().one()
    }

    // Forms a list of persons' ssn's that should be updated from DVV
    private fun getPersonSsnsToUpdate(): List<String> = jdbi.handle { h ->
        //language=sql
        h.createQuery("""
SELECT DISTINCT(social_security_number) from PERSON p JOIN (
SELECT head_of_child FROM fridge_child
WHERE daterange(start_date, end_date, '[]') @> current_date AND conflict = false) hoc ON p.id = hoc.head_of_child
            """.trimIndent())
                .mapTo<String>()
                .toList()
    }
}