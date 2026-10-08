// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { BatchSummary } from 'lib-common/generated/api-types/mcp'
import type { DeletionResult } from 'lib-common/generated/api-types/mcp'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { McpAuthorizationId } from 'lib-common/generated/api-types/shared'
import type { McpAuthorizationRequest } from 'lib-common/generated/api-types/mcp'
import type { McpAuthorizationResponse } from 'lib-common/generated/api-types/mcp'
import type { McpAuthorizationSummary } from 'lib-common/generated/api-types/mcp'
import type { McpClientId } from 'lib-common/generated/api-types/shared'
import type { McpConfigResponse } from 'lib-common/generated/api-types/mcp'
import type { McpOAuthClientInfo } from 'lib-common/generated/api-types/mcp'
import type { McpTestDataBatchDetails } from 'lib-common/generated/api-types/mcp'
import type { McpTestDataBatchId } from 'lib-common/generated/api-types/shared'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonBatchSummary } from 'lib-common/generated/api-types/mcp'
import { deserializeJsonMcpAuthorizationSummary } from 'lib-common/generated/api-types/mcp'
import { deserializeJsonMcpTestDataBatchDetails } from 'lib-common/generated/api-types/mcp'
import { uri } from 'lib-common/uri'


/**
* Generated from evaka.core.mcp.McpEmployeeController.createMcpAuthorization
*/
export async function createMcpAuthorization(
  request: {
    body: McpAuthorizationRequest
  }
): Promise<McpAuthorizationResponse> {
  const { data: json } = await client.request<JsonOf<McpAuthorizationResponse>>({
    url: uri`/employee/mcp/authorizations`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<McpAuthorizationRequest>
  })
  return json
}


/**
* Generated from evaka.core.mcp.McpEmployeeController.deleteMcpTestDataBatch
*/
export async function deleteMcpTestDataBatch(
  request: {
    id: McpTestDataBatchId
  }
): Promise<DeletionResult> {
  const { data: json } = await client.request<JsonOf<DeletionResult>>({
    url: uri`/employee/mcp/test-data/batches/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from evaka.core.mcp.McpEmployeeController.getMcpAuthorizations
*/
export async function getMcpAuthorizations(): Promise<McpAuthorizationSummary[]> {
  const { data: json } = await client.request<JsonOf<McpAuthorizationSummary[]>>({
    url: uri`/employee/mcp/authorizations`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonMcpAuthorizationSummary(e))
}


/**
* Generated from evaka.core.mcp.McpEmployeeController.getMcpConfig
*/
export async function getMcpConfig(): Promise<McpConfigResponse> {
  const { data: json } = await client.request<JsonOf<McpConfigResponse>>({
    url: uri`/employee/mcp/config`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from evaka.core.mcp.McpEmployeeController.getMcpOAuthClient
*/
export async function getMcpOAuthClient(
  request: {
    clientId: McpClientId,
    redirectUri: string
  }
): Promise<McpOAuthClientInfo> {
  const params = createUrlSearchParams(
    ['redirectUri', request.redirectUri.toString()]
  )
  const { data: json } = await client.request<JsonOf<McpOAuthClientInfo>>({
    url: uri`/employee/mcp/oauth-clients/${request.clientId}`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from evaka.core.mcp.McpEmployeeController.getMcpTestDataBatch
*/
export async function getMcpTestDataBatch(
  request: {
    id: McpTestDataBatchId
  }
): Promise<McpTestDataBatchDetails> {
  const { data: json } = await client.request<JsonOf<McpTestDataBatchDetails>>({
    url: uri`/employee/mcp/test-data/batches/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonMcpTestDataBatchDetails(json)
}


/**
* Generated from evaka.core.mcp.McpEmployeeController.getMcpTestDataBatchDeletionPreview
*/
export async function getMcpTestDataBatchDeletionPreview(
  request: {
    id: McpTestDataBatchId
  }
): Promise<DeletionResult> {
  const { data: json } = await client.request<JsonOf<DeletionResult>>({
    url: uri`/employee/mcp/test-data/batches/${request.id}/deletion-preview`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from evaka.core.mcp.McpEmployeeController.getMcpTestDataBatches
*/
export async function getMcpTestDataBatches(): Promise<BatchSummary[]> {
  const { data: json } = await client.request<JsonOf<BatchSummary[]>>({
    url: uri`/employee/mcp/test-data/batches`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonBatchSummary(e))
}


/**
* Generated from evaka.core.mcp.McpEmployeeController.revokeMcpAuthorization
*/
export async function revokeMcpAuthorization(
  request: {
    id: McpAuthorizationId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/mcp/authorizations/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}
