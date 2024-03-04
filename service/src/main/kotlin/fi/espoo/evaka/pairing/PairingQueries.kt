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
    val expires = clock.now().plusMinutes(expiresInMinutes)
    val challenge = generatePairingKey()
    return createQuery {
            sql(
                """
INSERT INTO pairing (unit_id, employee_id, expires, challenge_key)
VALUES (${bind(unitId)}, ${bind(employeeId)}, ${bind(expires)}, ${bind(challenge)})
RETURNING *
"""
            )
        }
        .exactlyOne<Pairing>()
}

fun Database.Transaction.challengePairing(clock: EvakaClock, challengeKey: String): Pairing {
    val response = generatePairingKey()
    val now = clock.now()
    return createQuery {
            sql(
                """
UPDATE pairing SET response_key = ${bind(response)}, status = 'WAITING_RESPONSE'
WHERE challenge_key = ${bind(challengeKey)} AND status = 'WAITING_CHALLENGE' AND expires > ${bind(now)} AND attempts <= ${bind(maxAttempts)}
RETURNING *
"""
            )
        }
        .exactlyOneOrNull<Pairing>() ?: throw NotFound("Valid pairing not found")
}

fun Database.Transaction.respondPairingChallengeCreateDevice(
    clock: EvakaClock,
    id: PairingId,
    challengeKey: String,
    responseKey: String
): Pairing {
    val defaultDeviceName = "Nimeämätön laite"
    val now = clock.now()

    return createQuery {
            sql(
                """
WITH target_pairing AS (
    SELECT p.id, unit_id, employee_id
    FROM pairing p
    WHERE p.id = ${bind(id)} AND p.challenge_key = ${bind(challengeKey)} AND p.response_key = ${bind(responseKey)} 
        AND p.status = 'WAITING_RESPONSE' AND p.expires > ${bind(now)} AND p.attempts <= ${bind(maxAttempts)}
), new_device AS (
    INSERT INTO mobile_device (unit_id, employee_id, name)
    SELECT target_pairing.unit_id, target_pairing.employee_id, ${bind(defaultDeviceName)}
    FROM target_pairing
    RETURNING mobile_device.id
)
UPDATE pairing SET status = 'READY', mobile_device_id = new_device.id
FROM target_pairing, new_device
WHERE pairing.id = target_pairing.id
RETURNING pairing.*
"""
            )
        }
        .exactlyOneOrNull<Pairing>() ?: throw NotFound("Valid pairing not found")
}

fun Database.Transaction.validatePairing(
    clock: EvakaClock,
    id: PairingId,
    challengeKey: String,
    responseKey: String
): MobileDeviceIdentity {
    val now = clock.now()
    val longTermToken = UUID.randomUUID()
    return createQuery {
            sql(
                """
WITH target_pairing AS (
    UPDATE pairing SET status = 'PAIRED'
    WHERE id = ${bind(id)} AND challenge_key = ${bind(challengeKey)} AND response_key = ${bind(responseKey)} AND status = 'READY' AND expires > ${bind(now)} AND attempts <= ${bind(maxAttempts)}
    RETURNING mobile_device_id
)
UPDATE mobile_device SET long_term_token = ${bind(longTermToken)}
WHERE id = (SELECT mobile_device_id FROM target_pairing)
RETURNING id, long_term_token
        """
            )
        }
        .exactlyOneOrNull<MobileDeviceIdentity>() ?: throw NotFound("Valid pairing not found")
}

fun Database.Read.fetchPairingReferenceIds(id: PairingId): Pair<DaycareId?, EmployeeId?> {
    return createQuery { sql("SELECT unit_id, employee_id FROM pairing WHERE id = ${bind(id)}") }
        .exactlyOneOrNull { columnPair("unit_id", "employee_id") }
        ?: throw NotFound("Pairing not found")
}

fun Database.Read.fetchPairingStatus(clock: EvakaClock, id: PairingId): PairingStatus {
    val now = clock.now()
    return createQuery {
            sql(
                """
SELECT status FROM pairing
WHERE id = ${bind(id)} AND expires > ${bind(now)} AND attempts <= ${bind(maxAttempts)}
"""
            )
        }
        .toList<PairingStatus>()
        .firstOrNull() ?: throw NotFound("Valid pairing not found")
}

fun Database.Transaction.incrementAttempts(id: PairingId, challengeKey: String) {
    createUpdate {
            sql(
                """
            UPDATE pairing
            SET attempts = attempts + 1
            WHERE id = ${bind(id)} OR challenge_key = ${bind(challengeKey)}
        """
            )
        }
        .execute()
}

const val distinguishableChars = "abcdefghkmnpqrstuvwxyz23456789"
const val keyLength = 12
val random: SecureRandom = SecureRandom()

fun generatePairingKey(): String =
    (1..keyLength)
        .map { random.nextInt(distinguishableChars.length) }
        .map { i -> distinguishableChars[i] }
        .joinToString(separator = "")
