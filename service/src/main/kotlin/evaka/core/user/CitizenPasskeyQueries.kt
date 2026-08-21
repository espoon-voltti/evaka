// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.user

import evaka.core.shared.CitizenPasskeyId
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.HelsinkiDateTime
import java.util.UUID

data class CitizenPasskey(
    val id: CitizenPasskeyId,
    val name: String,
    val createdAt: HelsinkiDateTime,
    val lastUsedAt: HelsinkiDateTime?,
    val deviceClass: DeviceClass,
    val operatingSystemName: String,
    val agentName: String,
)

data class PasskeyCredential(
    val id: CitizenPasskeyId,
    val citizenUserId: PersonId,
    val credentialId: ByteArray,
    val publicKey: ByteArray,
    val signatureCounter: Long,
)

data class NewPasskey(
    val credentialId: ByteArray,
    val publicKey: ByteArray,
    val signatureCounter: Long,
    val aaguid: UUID,
    val transports: List<String>,
    val name: String,
    val client: ParsedUserAgent,
)

fun Database.Read.getCitizenPasskeys(person: PersonId): List<CitizenPasskey> = createQuery {
    sql(
        """
SELECT id, name, created_at, last_used_at, device_class, operating_system_name, agent_name
FROM citizen_passkey
WHERE citizen_user_id = ${bind(person)}
ORDER BY created_at
"""
    )
}
    .toList()

fun Database.Read.getCitizenPasskeyCredentialIds(person: PersonId): List<ByteArray> = createQuery {
    sql("SELECT credential_id FROM citizen_passkey WHERE citizen_user_id = ${bind(person)}")
}
    .toList()

fun Database.Read.getPasskeyByCredentialId(credentialId: ByteArray): PasskeyCredential? =
    createQuery {
        sql(
            """
SELECT id, citizen_user_id, credential_id, public_key, signature_counter
FROM citizen_passkey
WHERE credential_id = ${bind(credentialId)}
"""
        )
    }
    .exactlyOneOrNull()

fun Database.Read.countCitizenPasskeys(person: PersonId): Int = createQuery {
    sql("SELECT count(*) FROM citizen_passkey WHERE citizen_user_id = ${bind(person)}")
}
    .exactlyOne()

fun Database.Transaction.upsertCitizenUserForPasskey(person: PersonId) = execute {
    sql("INSERT INTO citizen_user (id) VALUES (${bind(person)}) ON CONFLICT (id) DO NOTHING")
}

fun Database.Transaction.insertCitizenPasskey(
    person: PersonId,
    passkey: NewPasskey,
): CitizenPasskey = createUpdate {
    sql(
        """
INSERT INTO citizen_passkey (citizen_user_id, credential_id, public_key, signature_counter, aaguid, transports, name, device_class, operating_system_name, agent_name)
VALUES (
    ${bind(person)},
    ${bind(passkey.credentialId)},
    ${bind(passkey.publicKey)},
    ${bind(passkey.signatureCounter)},
    ${bind(passkey.aaguid)},
    ${bind(passkey.transports)},
    ${bind(passkey.name)},
    ${bind(passkey.client.deviceClass)},
    ${bind(passkey.client.operatingSystemName)},
    ${bind(passkey.client.agentName)}
)
RETURNING id, name, created_at, last_used_at, device_class, operating_system_name, agent_name
"""
    )
}
    .executeAndReturnGeneratedKeys()
    .exactlyOne()

fun Database.Transaction.updateCitizenPasskeyName(
    person: PersonId,
    id: CitizenPasskeyId,
    name: String,
): CitizenPasskey? = createUpdate {
    sql(
        """
UPDATE citizen_passkey
SET name = ${bind(name)}
WHERE id = ${bind(id)} AND citizen_user_id = ${bind(person)}
RETURNING id, name, created_at, last_used_at, device_class, operating_system_name, agent_name
"""
    )
}
    .executeAndReturnGeneratedKeys()
    .exactlyOneOrNull()

fun Database.Transaction.deleteCitizenPasskey(
    person: PersonId,
    id: CitizenPasskeyId,
): CitizenPasskey? = createUpdate {
    sql(
        """
DELETE FROM citizen_passkey
WHERE id = ${bind(id)} AND citizen_user_id = ${bind(person)}
RETURNING id, name, created_at, last_used_at, device_class, operating_system_name, agent_name
"""
    )
}
    .executeAndReturnGeneratedKeys()
    .exactlyOneOrNull()

fun Database.Transaction.updatePasskeyAfterLogin(
    id: CitizenPasskeyId,
    now: HelsinkiDateTime,
    signatureCounter: Long,
): Int = createUpdate {
    sql(
        """
UPDATE citizen_passkey
SET last_used_at = ${bind(now)}, signature_counter = ${bind(signatureCounter)}
WHERE id = ${bind(id)}
"""
    )
}
    .execute()

fun Database.Transaction.upsertPasskeyRegistration(
    person: PersonId,
    options: String,
    expiresAt: HelsinkiDateTime,
) = execute {
    sql(
        """
INSERT INTO citizen_passkey_registration (person_id, options, expires_at)
VALUES (${bind(person)}, ${bind(options)}::jsonb, ${bind(expiresAt)})
ON CONFLICT (person_id) DO UPDATE SET
    options = excluded.options,
    expires_at = excluded.expires_at,
    created_at = now()
"""
    )
}

private data class PasskeyRegistration(val options: String, val expiresAt: HelsinkiDateTime)

fun Database.Transaction.consumePasskeyRegistration(
    person: PersonId,
    now: HelsinkiDateTime,
): String? = createUpdate {
    sql(
        """
DELETE FROM citizen_passkey_registration
WHERE person_id = ${bind(person)}
RETURNING options::text AS options, expires_at
"""
    )
}
    .executeAndReturnGeneratedKeys()
    .exactlyOneOrNull<PasskeyRegistration>()
    ?.takeIf { it.expiresAt > now }
    ?.options
