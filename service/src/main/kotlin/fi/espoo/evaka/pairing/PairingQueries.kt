// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.utils.zoneId
import org.jdbi.v3.core.kotlin.mapTo
import java.security.SecureRandom
import java.time.ZonedDateTime
import java.util.UUID

const val maxAttempts = 100 // additional brute-force protection
const val expiresInMinutes = 60L

fun initPairing(tx: Database.Transaction, unitId: UUID): Pairing {
    // language=sql
    val sql =
        """
            INSERT INTO pairing (unit_id, expires, challenge_key) 
            VALUES (:unitId, :expires, :challenge)
            RETURNING *
        """.trimIndent()

    return tx.createQuery(sql)
        .bind("unitId", unitId)
        .bind("expires", ZonedDateTime.now(zoneId).plusMinutes(expiresInMinutes).toInstant())
        .bind("challenge", generatePairingKey())
        .mapTo<Pairing>()
        .first()
}

fun challengePairing(tx: Database.Transaction, challengeKey: String): Pairing {
    // language=sql
    val sql =
        """
            UPDATE pairing SET response_key = :response, status = 'WAITING_RESPONSE'
            WHERE challenge_key = :challenge AND status = 'WAITING_CHALLENGE' AND expires > :now AND attempts <= :maxAttempts
            RETURNING *
        """.trimIndent()

    return tx.createQuery(sql)
        .bind("challenge", challengeKey)
        .bind("response", generatePairingKey())
        .bind("now", ZonedDateTime.now(zoneId).toInstant())
        .bind("maxAttempts", maxAttempts)
        .mapTo<Pairing>()
        .firstOrNull() ?: throw NotFound("Valid pairing not found")
}

fun respondPairingChallenge(tx: Database.Transaction, id: UUID, challengeKey: String, responseKey: String): Pairing {
    // language=sql
    val sql =
        """
            UPDATE pairing SET status = 'READY'
            WHERE id = :id AND challenge_key = :challenge AND response_key = :response AND status = 'WAITING_RESPONSE' AND expires > :now AND attempts <= :maxAttempts
            RETURNING *
        """.trimIndent()

    return tx.createQuery(sql)
        .bind("id", id)
        .bind("challenge", challengeKey)
        .bind("response", responseKey)
        .bind("now", ZonedDateTime.now(zoneId).toInstant())
        .bind("maxAttempts", maxAttempts)
        .mapTo<Pairing>()
        .firstOrNull() ?: throw NotFound("Valid pairing not found")
}

fun validatePairing(tx: Database.Transaction, id: UUID, challengeKey: String, responseKey: String) {
    // language=sql
    val sql =
        """
            UPDATE pairing SET status = 'PAIRED'
            WHERE id = :id AND challenge_key = :challenge AND response_key = :response AND status = 'READY' AND expires > :now AND attempts <= :maxAttempts
        """.trimIndent()

    tx.createUpdate(sql)
        .bind("id", id)
        .bind("challenge", challengeKey)
        .bind("response", responseKey)
        .bind("now", ZonedDateTime.now(zoneId).toInstant())
        .bind("maxAttempts", maxAttempts)
        .execute()
        .takeIf { it > 0 } ?: throw NotFound("Valid pairing not found")
}

fun fetchPairingStatus(tx: Database.Read, id: UUID): PairingStatus {
    // language=sql
    val sql =
        """
            SELECT status FROM pairing
            WHERE id = :id AND expires > :now AND attempts <= :maxAttempts
        """.trimIndent()

    return tx.createQuery(sql)
        .bind("id", id)
        .bind("now", ZonedDateTime.now(zoneId).toInstant())
        .bind("maxAttempts", maxAttempts)
        .mapTo<PairingStatus>()
        .list()
        .firstOrNull() ?: throw NotFound("Valid pairing not found")
}

fun incrementAttempts(tx: Database.Transaction, id: UUID, challengeKey: String) {
    // language=sql
    val sql =
        """
            UPDATE pairing
            SET attempts = attempts + 1
            WHERE id = :id OR challenge_key = :challenge
        """.trimIndent()

    tx.createUpdate(sql)
        .bind("id", id)
        .bind("challenge", challengeKey)
        .execute()
}

const val distinguishableChars = "abcdefghkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
const val keyLength = 10
val random: SecureRandom = SecureRandom.getInstanceStrong()
fun generatePairingKey(): String = (1..keyLength)
    .map { random.nextInt(distinguishableChars.length) }
    .map { i -> distinguishableChars[i] }
    .joinToString(separator = "")
