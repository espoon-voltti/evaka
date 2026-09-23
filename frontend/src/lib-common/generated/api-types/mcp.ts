// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { EvakaUserId } from './shared'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import type { McpAuthorizationId } from './shared'
import type { McpClientId } from './shared'
import type { McpTestDataBatchId } from './shared'
import type { UUID } from '../../types'

/**
* Generated from evaka.core.mcp.McpTestDataService.BatchSummary
*/
export interface BatchSummary {
  clientName: string | null
  createdAt: HelsinkiDateTime
  createdBy: EvakaUserId
  createdByName: string
  description: string
  entityCounts: Partial<Record<string, number>>
  id: McpTestDataBatchId
  name: string
  totalEntities: number
}

/**
* Generated from evaka.core.mcp.McpTestDataService.DeletionResult
*/
export interface DeletionResult {
  batchId: McpTestDataBatchId
  batchName: string
  deletedRowCounts: Partial<Record<string, number>>
  trackedEntities: number
  untrackedRowCounts: Partial<Record<string, number>>
}

/**
* Generated from evaka.core.mcp.McpEmployeeController.McpAuthorizationRequest
*/
export interface McpAuthorizationRequest {
  clientId: McpClientId
  codeChallenge: string
  codeChallengeMethod: string
  redirectUri: string
  resource: string | null
  scope: string | null
  state: string | null
  validityDays: number
}

/**
* Generated from evaka.core.mcp.McpEmployeeController.McpAuthorizationResponse
*/
export interface McpAuthorizationResponse {
  redirectUrl: string
}

/**
* Generated from evaka.core.mcp.McpAuthorizationSummary
*/
export interface McpAuthorizationSummary {
  active: boolean
  clientId: McpClientId
  clientName: string
  clientUri: string | null
  createdAt: HelsinkiDateTime
  expiresAt: HelsinkiDateTime
  id: McpAuthorizationId
  lastUsedAt: HelsinkiDateTime | null
  revokedAt: HelsinkiDateTime | null
  tokenIssued: boolean
}

/**
* Generated from evaka.core.mcp.McpEmployeeController.McpConfigResponse
*/
export interface McpConfigResponse {
  defaultValidityDays: number
  serverUrl: string
  validityOptionsDays: number[]
}

/**
* Generated from evaka.core.mcp.McpEmployeeController.McpOAuthClientInfo
*/
export interface McpOAuthClientInfo {
  clientName: string
  clientUri: string | null
  id: McpClientId
  redirectUriAccepted: boolean
  redirectUris: string[]
}

/**
* Generated from evaka.core.mcp.McpTestDataBatch
*/
export interface McpTestDataBatch {
  clientName: string | null
  createdAt: HelsinkiDateTime
  createdBy: EvakaUserId
  createdByName: string
  description: string
  id: McpTestDataBatchId
  name: string
}

/**
* Generated from evaka.core.mcp.McpEmployeeController.McpTestDataBatchDetails
*/
export interface McpTestDataBatchDetails {
  batch: McpTestDataBatch
  entities: McpTestDataEntity[]
}

/**
* Generated from evaka.core.mcp.McpTestDataEntity
*/
export interface McpTestDataEntity {
  batchId: McpTestDataBatchId
  createdAt: HelsinkiDateTime
  description: string
  entityId: UUID
  tableName: string
}


export function deserializeJsonBatchSummary(json: JsonOf<BatchSummary>): BatchSummary {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt)
  }
}


export function deserializeJsonMcpAuthorizationSummary(json: JsonOf<McpAuthorizationSummary>): McpAuthorizationSummary {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    expiresAt: HelsinkiDateTime.parseIso(json.expiresAt),
    lastUsedAt: (json.lastUsedAt != null) ? HelsinkiDateTime.parseIso(json.lastUsedAt) : null,
    revokedAt: (json.revokedAt != null) ? HelsinkiDateTime.parseIso(json.revokedAt) : null
  }
}


export function deserializeJsonMcpTestDataBatch(json: JsonOf<McpTestDataBatch>): McpTestDataBatch {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt)
  }
}


export function deserializeJsonMcpTestDataBatchDetails(json: JsonOf<McpTestDataBatchDetails>): McpTestDataBatchDetails {
  return {
    ...json,
    batch: deserializeJsonMcpTestDataBatch(json.batch),
    entities: json.entities.map(e => deserializeJsonMcpTestDataEntity(e))
  }
}


export function deserializeJsonMcpTestDataEntity(json: JsonOf<McpTestDataEntity>): McpTestDataEntity {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt)
  }
}
