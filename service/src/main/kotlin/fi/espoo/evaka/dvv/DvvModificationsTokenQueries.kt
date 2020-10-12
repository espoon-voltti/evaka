import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo

fun storeDvvModificationToken(jdbi: Jdbi, token: String, nextToken: String, ssnsSent: Int, modificationsReceived: Int) = jdbi.transaction { h ->
    h.createUpdate(
        """
INSERT INTO dvv_modification_token (token, next_token, ssns_sent, modifications_received) 
VALUES (:token, :nextToken, :ssnsSent, :modificationsReceived)
        """.trimIndent()
    )
        .bind("token", token)
        .bind("nextToken", nextToken)
        .bind("ssnsSent", ssnsSent)
        .bind("modificationsReceived", modificationsReceived)
        .execute()
}

fun getNextDvvModificationToken(jdbi: Jdbi): String = jdbi.handle { h ->
    h.createQuery(
        """
SELECT next_token 
FROM dvv_modification_token
ORDER BY created DESC
LIMIT 1
        """.trimIndent()
    ).mapTo<String>().one()
}

fun getDvvModificationToken(jdbi: Jdbi, token: String): DvvModificationToken? = jdbi.handle { h ->
    h.createQuery("""SELECT * FROM dvv_modification_token WHERE token = :token""")
        .bind("token", token)
        .mapTo<DvvModificationToken>()
        .one()
}

fun deleteDvvModificationToken(jdbi: Jdbi, token: String) = jdbi.handle { h ->
    h.createUpdate("""DELETE FROM dvv_modification_token WHERE token = :token""").bind("token", token)
}

data class DvvModificationToken(
    val token: String,
    val nextToken: String,
    val ssnsSent: Int,
    val modificationsReceived: Int
)
