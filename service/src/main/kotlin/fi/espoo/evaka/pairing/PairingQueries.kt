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

fun Database.Transaction.initPairing(unitId: UUID): Pairing {
    // language=sql
    val sql =
        """
            INSERT INTO pairing (unit_id, expires, challenge_key) 
            VALUES (:unitId, :expires, :challenge)
            RETURNING *
        """.trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .bind("expires", ZonedDateTime.now(zoneId).plusMinutes(expiresInMinutes).toInstant())
        .bind("challenge", generatePairingKey())
        .mapTo<Pairing>()
        .first()
}

fun Database.Transaction.challengePairing(challengeKey: String): Pairing {
    // language=sql
    val sql =
        """
            UPDATE pairing SET response_key = :response, status = 'WAITING_RESPONSE'
            WHERE challenge_key = :challenge AND status = 'WAITING_CHALLENGE' AND expires > :now AND attempts <= :maxAttempts
            RETURNING *
        """.trimIndent()

    return createQuery(sql)
        .bind("challenge", challengeKey)
        .bind("response", generatePairingKey())
        .bind("now", ZonedDateTime.now(zoneId).toInstant())
        .bind("maxAttempts", maxAttempts)
        .mapTo<Pairing>()
        .firstOrNull() ?: throw NotFound("Valid pairing not found")
}

fun Database.Transaction.respondPairingChallengeCreateDevice(id: UUID, challengeKey: String, responseKey: String): Pairing {
    val defaultDeviceName = "Nimeämätön laite"

    // language=sql
    val sql =
        """
            WITH target_pairing AS (
                SELECT p.id, unit_id, u.name AS unit_name
                FROM pairing p
                JOIN daycare u ON u.id = p.unit_id
                WHERE p.id = :id AND p.challenge_key = :challenge AND p.response_key = :response 
                    AND p.status = 'WAITING_RESPONSE' AND p.expires > :now AND p.attempts <= :maxAttempts
            ), new_employee AS (
                INSERT INTO employee (first_name, last_name, email, aad_object_id)
                SELECT :name, unit_name, null, null
                FROM target_pairing
                RETURNING employee.id
            ), new_device AS (
                INSERT INTO mobile_device (id, unit_id, name)
                SELECT new_employee.id, target_pairing.unit_id, :name
                FROM new_employee, target_pairing
                RETURNING mobile_device.id
            )
            UPDATE pairing SET status = 'READY', mobile_device_id = new_device.id
            FROM target_pairing, new_device
            WHERE pairing.id = target_pairing.id
            RETURNING pairing.*
        """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .bind("challenge", challengeKey)
        .bind("response", responseKey)
        .bind("name", defaultDeviceName)
        .bind("now", ZonedDateTime.now(zoneId).toInstant())
        .bind("maxAttempts", maxAttempts)
        .mapTo<Pairing>()
        .firstOrNull() ?: throw NotFound("Valid pairing not found")
}

fun Database.Transaction.validatePairing(id: UUID, challengeKey: String, responseKey: String): MobileDeviceIdentity {
    // language=sql
    val sql =
        """
WITH target_pairing AS (
    UPDATE pairing SET status = 'PAIRED'
    WHERE id = :id AND challenge_key = :challenge AND response_key = :response AND status = 'READY' AND expires > :now AND attempts <= :maxAttempts
    RETURNING mobile_device_id
)
UPDATE mobile_device SET long_term_token = :longTermToken
WHERE id = (SELECT mobile_device_id FROM target_pairing)
RETURNING id, long_term_token
        """

    return createQuery(sql)
        .bind("id", id)
        .bind("challenge", challengeKey)
        .bind("response", responseKey)
        .bind("now", ZonedDateTime.now(zoneId).toInstant())
        .bind("maxAttempts", maxAttempts)
        .bind("longTermToken", UUID.randomUUID())
        .mapTo<MobileDeviceIdentity>()
        .firstOrNull() ?: throw NotFound("Valid pairing not found")
}

fun Database.Read.fetchPairingStatus(id: UUID): PairingStatus {
    // language=sql
    val sql =
        """
            SELECT status FROM pairing
            WHERE id = :id AND expires > :now AND attempts <= :maxAttempts
        """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .bind("now", ZonedDateTime.now(zoneId).toInstant())
        .bind("maxAttempts", maxAttempts)
        .mapTo<PairingStatus>()
        .list()
        .firstOrNull() ?: throw NotFound("Valid pairing not found")
}

fun Database.Transaction.incrementAttempts(id: UUID, challengeKey: String) {
    // language=sql
    val sql =
        """
            UPDATE pairing
            SET attempts = attempts + 1
            WHERE id = :id OR challenge_key = :challenge
        """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
        .bind("challenge", challengeKey)
        .execute()
}

const val distinguishableChars = "abcdefghkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
const val keyLength = 10
val random: SecureRandom = SecureRandom()
fun generatePairingKey(): String = (1..keyLength)
    .map { random.nextInt(distinguishableChars.length) }
    .map { i -> distinguishableChars[i] }
    .joinToString(separator = "")
