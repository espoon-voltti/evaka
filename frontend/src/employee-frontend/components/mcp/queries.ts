// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  createMcpAuthorization,
  deleteMcpTestDataBatch,
  getMcpAuthorizations,
  getMcpConfig,
  getMcpOAuthClient,
  getMcpTestDataBatch,
  getMcpTestDataBatchDeletionPreview,
  getMcpTestDataBatches,
  revokeMcpAuthorization
} from '../../generated/api-clients/mcp'

const q = new Queries()

export const mcpConfigQuery = q.query(getMcpConfig)
export const mcpOAuthClientQuery = q.query(getMcpOAuthClient)
export const mcpAuthorizationsQuery = q.query(getMcpAuthorizations)
export const createMcpAuthorizationMutation = q.mutation(
  createMcpAuthorization,
  [mcpAuthorizationsQuery.prefix]
)
export const revokeMcpAuthorizationMutation = q.mutation(
  revokeMcpAuthorization,
  [mcpAuthorizationsQuery.prefix]
)
export const mcpTestDataBatchesQuery = q.query(getMcpTestDataBatches)
export const mcpTestDataBatchQuery = q.query(getMcpTestDataBatch)
export const mcpTestDataBatchDeletionPreviewQuery = q.query(
  getMcpTestDataBatchDeletionPreview
)
export const deleteMcpTestDataBatchMutation = q.mutation(
  deleteMcpTestDataBatch,
  [
    mcpTestDataBatchesQuery.prefix,
    mcpTestDataBatchQuery.prefix,
    mcpTestDataBatchDeletionPreviewQuery.prefix
  ]
)
