// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import java.security.SecureRandom
import java.util.UUID

const val maxAttempts = 100 // additional brute-force protection
const val expiresInMinutes = 60L

fun Database.Transaction.initPairing(
    clock: EvakaClock,
    unitId: DaycareId? = null,
    employeeId: EmployeeId? = null
): Pairing {
    // language=sql
    val sql =
        """
            INSERT INTO pairing (unit_id, employee_id, expires, challenge_key)
            VALUES (:unitId, :employeeId, :expires, :challenge)
            RETURNING *
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("unitId", unitId)
        .bind("employeeId", employeeId)
        .bind("expires", clock.now().plusMinutes(expiresInMinutes))
        .bind("challenge", generatePairingKey())
        .exactlyOne<Pairing>()
}

fun Database.Transaction.challengePairing(clock: EvakaClock, challengeKey: String): Pairing {
    // language=sql
    val sql =
        """
            UPDATE pairing SET response_key = :response, status = 'WAITING_RESPONSE'
            WHERE challenge_key = :challenge AND status = 'WAITING_CHALLENGE' AND expires > :now AND attempts <= :maxAttempts
            RETURNING *
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("challenge", challengeKey)
        .bind("response", generatePairingKey())
        .bind("now", clock.now())
        .bind("maxAttempts", maxAttempts)
        .exactlyOneOrNull<Pairing>() ?: throw NotFound("Valid pairing not found")
}

fun Database.Transaction.respondPairingChallengeCreateDevice(
    clock: EvakaClock,
    id: PairingId,
    challengeKey: String,
    responseKey: String
): Pairing {
    val defaultDeviceName = "Nimeämätön laite"

    // language=sql
    val sql =
        """
            WITH target_pairing AS (
                SELECT p.id, unit_id, employee_id
                FROM pairing p
                WHERE p.id = :id AND p.challenge_key = :challenge AND p.response_key = :response 
                    AND p.status = 'WAITING_RESPONSE' AND p.expires > :now AND p.attempts <= :maxAttempts
            ), new_device AS (
                INSERT INTO mobile_device (unit_id, employee_id, name)
                SELECT target_pairing.unit_id, target_pairing.employee_id, :name
                FROM target_pairing
                RETURNING mobile_device.id
            )
            UPDATE pairing SET status = 'READY', mobile_device_id = new_device.id
            FROM target_pairing, new_device
            WHERE pairing.id = target_pairing.id
            RETURNING pairing.*
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("id", id)
        .bind("challenge", challengeKey)
        .bind("response", responseKey)
        .bind("name", defaultDeviceName)
        .bind("now", clock.now())
        .bind("maxAttempts", maxAttempts)
        .exactlyOneOrNull<Pairing>() ?: throw NotFound("Valid pairing not found")
}

fun Database.Transaction.validatePairing(
    clock: EvakaClock,
    id: PairingId,
    challengeKey: String,
    responseKey: String
): MobileDeviceIdentity {
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

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("id", id)
        .bind("challenge", challengeKey)
        .bind("response", responseKey)
        .bind("now", clock.now())
        .bind("maxAttempts", maxAttempts)
        .bind("longTermToken", UUID.randomUUID())
        .exactlyOneOrNull<MobileDeviceIdentity>() ?: throw NotFound("Valid pairing not found")
}

fun Database.Read.fetchPairingReferenceIds(id: PairingId): Pair<DaycareId?, EmployeeId?> {
    @Suppress("DEPRECATION")
    return createQuery("SELECT unit_id, employee_id FROM pairing WHERE id = :id")
        .bind("id", id)
        .exactlyOneOrNull { columnPair("unit_id", "employee_id") }
        ?: throw NotFound("Pairing not found")
}

fun Database.Read.fetchPairingStatus(clock: EvakaClock, id: PairingId): PairingStatus {
    // language=sql
    val sql =
        """
            SELECT status FROM pairing
            WHERE id = :id AND expires > :now AND attempts <= :maxAttempts
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("id", id)
        .bind("now", clock.now())
        .bind("maxAttempts", maxAttempts)
        .toList<PairingStatus>()
        .firstOrNull() ?: throw NotFound("Valid pairing not found")
}

fun Database.Transaction.incrementAttempts(id: PairingId, challengeKey: String) {
    // language=sql
    val sql =
        """
            UPDATE pairing
            SET attempts = attempts + 1
            WHERE id = :id OR challenge_key = :challenge
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    createUpdate(sql).bind("id", id).bind("challenge", challengeKey).execute()
}

const val distinguishableChars = "abcdefghkmnpqrstuvwxyz23456789"
const val keyLength = 12
val random: SecureRandom = SecureRandom()

fun generatePairingKey(): String =
    (1..keyLength)
        .map { random.nextInt(distinguishableChars.length) }
        .map { i -> distinguishableChars[i] }
        .joinToString(separator = "")
