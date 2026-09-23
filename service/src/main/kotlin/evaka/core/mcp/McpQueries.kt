// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.shared.EmployeeId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.McpAuthorizationId
import evaka.core.shared.McpClientId
import evaka.core.shared.McpTestDataBatchId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.HelsinkiDateTime
import java.util.UUID

data class McpClient(
    val id: McpClientId,
    val createdAt: HelsinkiDateTime,
    val clientName: String,
    val clientUri: String?,
    val softwareId: String?,
    val softwareVersion: String?,
    val redirectUris: List<String>,
    val tokenEndpointAuthMethod: String,
    val clientSecretHash: String?,
)

fun Database.Transaction.insertMcpClient(
    clientName: String,
    clientUri: String?,
    softwareId: String?,
    softwareVersion: String?,
    redirectUris: List<String>,
    tokenEndpointAuthMethod: String,
    clientSecretHash: String?,
    registrationIp: String?,
    now: HelsinkiDateTime,
    id: McpClientId = McpClientId(UUID.randomUUID()),
): McpClientId = createUpdate {
    sql(
        """
INSERT INTO mcp_client (id, created_at, client_name, client_uri, software_id, software_version, redirect_uris, token_endpoint_auth_method, client_secret_hash, registration_ip)
VALUES (${bind(id)}, ${bind(now)}, ${bind(clientName)}, ${bind(clientUri)}, ${bind(softwareId)}, ${bind(softwareVersion)}, ${bind(redirectUris)}, ${bind(tokenEndpointAuthMethod)}, ${bind(clientSecretHash)}, ${bind(registrationIp)})
RETURNING id
"""
    )
}
    .executeAndReturnGeneratedKeys()
    .exactlyOne()

fun Database.Read.getMcpClient(id: McpClientId): McpClient? = createQuery {
    sql(
        """
SELECT id, created_at, client_name, client_uri, software_id, software_version, redirect_uris, token_endpoint_auth_method, client_secret_hash
FROM mcp_client
WHERE id = ${bind(id)}
"""
    )
}
    .exactlyOneOrNull()

data class McpAuthorization(
    val id: McpAuthorizationId,
    val createdAt: HelsinkiDateTime,
    val clientId: McpClientId,
    val employeeId: EmployeeId,
    val scope: String,
    val expiresAt: HelsinkiDateTime,
    val revokedAt: HelsinkiDateTime?,
    val codeHash: String?,
    val codeExpiresAt: HelsinkiDateTime?,
    val codeChallenge: String,
    val redirectUri: String,
    val resource: String?,
    val accessTokenHash: String?,
    val tokenIssuedAt: HelsinkiDateTime?,
    val lastUsedAt: HelsinkiDateTime?,
) {
    fun isActive(now: HelsinkiDateTime) = revokedAt == null && expiresAt.isAfter(now)
}

private const val AUTHORIZATION_COLUMNS =
    """
id, created_at, client_id, employee_id, scope, expires_at, revoked_at, code_hash, code_expires_at,
code_challenge, redirect_uri, resource, access_token_hash, token_issued_at, last_used_at
"""

fun Database.Transaction.insertMcpAuthorization(
    clientId: McpClientId,
    employeeId: EmployeeId,
    scope: String,
    expiresAt: HelsinkiDateTime,
    codeHash: String,
    codeExpiresAt: HelsinkiDateTime,
    codeChallenge: String,
    redirectUri: String,
    resource: String?,
    now: HelsinkiDateTime,
): McpAuthorizationId = createUpdate {
    sql(
        """
INSERT INTO mcp_authorization (created_at, client_id, employee_id, scope, expires_at, code_hash, code_expires_at, code_challenge, redirect_uri, resource)
VALUES (${bind(now)}, ${bind(clientId)}, ${bind(employeeId)}, ${bind(scope)}, ${bind(expiresAt)}, ${bind(codeHash)}, ${bind(codeExpiresAt)}, ${bind(codeChallenge)}, ${bind(redirectUri)}, ${bind(resource)})
RETURNING id
"""
    )
}
    .executeAndReturnGeneratedKeys()
    .exactlyOne()

fun Database.Read.getMcpAuthorizationByCodeHash(codeHash: String): McpAuthorization? = createQuery {
    sql("SELECT $AUTHORIZATION_COLUMNS FROM mcp_authorization WHERE code_hash = ${bind(codeHash)}")
}
    .exactlyOneOrNull()

fun Database.Read.getMcpAuthorizationByAccessTokenHash(tokenHash: String): McpAuthorization? =
    createQuery {
        sql(
            "SELECT $AUTHORIZATION_COLUMNS FROM mcp_authorization WHERE access_token_hash = ${bind(tokenHash)}"
        )
    }
    .exactlyOneOrNull()

fun Database.Read.getMcpAuthorization(id: McpAuthorizationId): McpAuthorization? = createQuery {
    sql("SELECT $AUTHORIZATION_COLUMNS FROM mcp_authorization WHERE id = ${bind(id)}")
}
    .exactlyOneOrNull()

/** Consumes the authorization code and stores the hash of the issued access token */
fun Database.Transaction.issueMcpAccessToken(
    id: McpAuthorizationId,
    accessTokenHash: String,
    now: HelsinkiDateTime,
) = createUpdate {
    sql(
        """
UPDATE mcp_authorization
SET code_hash = NULL, code_expires_at = NULL, access_token_hash = ${bind(accessTokenHash)}, token_issued_at = ${bind(now)}
WHERE id = ${bind(id)} AND code_hash IS NOT NULL
"""
    )
}
    .updateExactlyOne()

fun Database.Transaction.updateMcpAuthorizationLastUsed(
    id: McpAuthorizationId,
    now: HelsinkiDateTime,
) = execute {
    sql("UPDATE mcp_authorization SET last_used_at = ${bind(now)} WHERE id = ${bind(id)}")
}

fun Database.Transaction.revokeMcpAuthorization(
    id: McpAuthorizationId,
    employeeId: EmployeeId,
    now: HelsinkiDateTime,
): Boolean =
    createUpdate {
        sql(
            """
UPDATE mcp_authorization
SET revoked_at = ${bind(now)}
WHERE id = ${bind(id)} AND employee_id = ${bind(employeeId)} AND revoked_at IS NULL
"""
        )
    }
        .executeAndReturnCount() > 0

data class McpAuthorizationSummary(
    val id: McpAuthorizationId,
    val clientId: McpClientId,
    val clientName: String,
    val clientUri: String?,
    val createdAt: HelsinkiDateTime,
    val expiresAt: HelsinkiDateTime,
    val revokedAt: HelsinkiDateTime?,
    val lastUsedAt: HelsinkiDateTime?,
    /** False if the authorization code was never exchanged for an access token */
    val tokenIssued: Boolean,
    val active: Boolean,
)

fun Database.Read.getMcpAuthorizationsOfEmployee(
    employeeId: EmployeeId,
    now: HelsinkiDateTime,
): List<McpAuthorizationSummary> = createQuery {
    sql(
        """
SELECT
    a.id, a.client_id, c.client_name, c.client_uri, a.created_at, a.expires_at, a.revoked_at, a.last_used_at,
    a.access_token_hash IS NOT NULL AS token_issued,
    (a.revoked_at IS NULL AND a.expires_at > ${bind(now)} AND a.access_token_hash IS NOT NULL) AS active
FROM mcp_authorization a
JOIN mcp_client c ON c.id = a.client_id
WHERE a.employee_id = ${bind(employeeId)}
ORDER BY a.created_at DESC
"""
    )
}
    .toList()

/** Removes authorizations whose code was never exchanged and has expired long ago */
fun Database.Transaction.deleteStaleMcpAuthorizations(now: HelsinkiDateTime) = execute {
    sql(
        """
DELETE FROM mcp_authorization
WHERE access_token_hash IS NULL AND code_expires_at < ${bind(now.minusDays(1))}
"""
    )
}

data class McpTestDataBatch(
    val id: McpTestDataBatchId,
    val createdAt: HelsinkiDateTime,
    val createdBy: EvakaUserId,
    val createdByName: String,
    val clientName: String?,
    val name: String,
    val description: String,
)

fun Database.Read.getMcpTestDataBatch(id: McpTestDataBatchId): McpTestDataBatch? = createQuery {
    sql(
        """
SELECT b.id, b.created_at, b.created_by, u.name AS created_by_name, c.client_name, b.name, b.description
FROM mcp_test_data_batch b
JOIN evaka_user u ON u.id = b.created_by
LEFT JOIN mcp_authorization a ON a.id = b.authorization_id
LEFT JOIN mcp_client c ON c.id = a.client_id
WHERE b.id = ${bind(id)}
"""
    )
}
    .exactlyOneOrNull()

fun Database.Read.getMcpTestDataBatchByName(
    name: String,
    createdBy: EvakaUserId,
): McpTestDataBatch? = createQuery {
    sql(
        """
SELECT b.id, b.created_at, b.created_by, u.name AS created_by_name, c.client_name, b.name, b.description
FROM mcp_test_data_batch b
JOIN evaka_user u ON u.id = b.created_by
LEFT JOIN mcp_authorization a ON a.id = b.authorization_id
LEFT JOIN mcp_client c ON c.id = a.client_id
WHERE b.name = ${bind(name)} AND b.created_by = ${bind(createdBy)}
"""
    )
}
    .exactlyOneOrNull()

fun Database.Transaction.insertMcpTestDataBatch(
    name: String,
    description: String,
    createdBy: EvakaUserId,
    authorizationId: McpAuthorizationId?,
    now: HelsinkiDateTime,
): McpTestDataBatchId = createUpdate {
    sql(
        """
INSERT INTO mcp_test_data_batch (created_at, created_by, authorization_id, name, description)
VALUES (${bind(now)}, ${bind(createdBy)}, ${bind(authorizationId)}, ${bind(name)}, ${bind(description)})
ON CONFLICT (created_by, name) DO UPDATE SET name = EXCLUDED.name
RETURNING id
"""
    )
}
    .executeAndReturnGeneratedKeys()
    .exactlyOne()

fun Database.Read.getMcpTestDataEntityBatch(
    tableName: String,
    entityId: UUID,
): McpTestDataBatchId? = createQuery {
    sql(
        "SELECT batch_id FROM mcp_test_data_entity WHERE table_name = ${bind(tableName)} AND entity_id = ${bind(entityId)}"
    )
}
    .exactlyOneOrNull<McpTestDataBatchId>()

fun Database.Transaction.insertMcpTestDataEntity(
    batchId: McpTestDataBatchId,
    tableName: String,
    entityId: UUID,
    description: String,
    now: HelsinkiDateTime,
) = execute {
    sql(
        """
INSERT INTO mcp_test_data_entity (created_at, batch_id, table_name, entity_id, description)
VALUES (${bind(now)}, ${bind(batchId)}, ${bind(tableName)}, ${bind(entityId)}, ${bind(description)})
ON CONFLICT (table_name, entity_id) DO NOTHING
"""
    )
}

data class McpTestDataEntity(
    val batchId: McpTestDataBatchId,
    val createdAt: HelsinkiDateTime,
    val tableName: String,
    val entityId: UUID,
    val description: String,
)

fun Database.Read.getMcpTestDataEntities(batchId: McpTestDataBatchId): List<McpTestDataEntity> =
    createQuery {
        sql(
            """
SELECT batch_id, created_at, table_name, entity_id, description
FROM mcp_test_data_entity
WHERE batch_id = ${bind(batchId)}
ORDER BY created_at, table_name
"""
        )
    }
    .toList()

data class McpTestDataEntityCount(
    val batchId: McpTestDataBatchId,
    val tableName: String,
    val count: Int,
)

fun Database.Read.getMcpTestDataBatches(createdBy: EvakaUserId? = null): List<McpTestDataBatch> =
    createQuery {
        sql(
            """
SELECT b.id, b.created_at, b.created_by, u.name AS created_by_name, c.client_name, b.name, b.description
FROM mcp_test_data_batch b
JOIN evaka_user u ON u.id = b.created_by
LEFT JOIN mcp_authorization a ON a.id = b.authorization_id
LEFT JOIN mcp_client c ON c.id = a.client_id
WHERE (${bind(createdBy)}::uuid IS NULL OR b.created_by = ${bind(createdBy)})
ORDER BY b.created_at DESC
"""
        )
    }
    .toList()

fun Database.Read.getMcpTestDataEntityCounts(): List<McpTestDataEntityCount> = createQuery {
    sql(
        """
SELECT batch_id, table_name, count(*) AS count
FROM mcp_test_data_entity
GROUP BY batch_id, table_name
ORDER BY table_name
"""
    )
}
    .toList()

fun Database.Transaction.deleteMcpTestDataBatch(id: McpTestDataBatchId) = execute {
    sql("DELETE FROM mcp_test_data_batch WHERE id = ${bind(id)}")
}
