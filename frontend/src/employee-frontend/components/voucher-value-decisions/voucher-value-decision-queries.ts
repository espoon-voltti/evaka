// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getVoucherValueDecisionMetadata } from '../../generated/api-clients/caseprocess'
import {
  ignoreVoucherValueDecisionDrafts,
  searchVoucherValueDecisions,
  sendVoucherValueDecisionDrafts,
  unignoreVoucherValueDecisionDrafts
} from '../../generated/api-clients/invoicing'

const q = new Queries()

export const searchVoucherValueDecisionsQuery = q.query(
  searchVoucherValueDecisions
)

export const voucherValueDecisionMetadataQuery = q.query(
  getVoucherValueDecisionMetadata
)

export const sendVoucherValueDecisionDraftsMutation = q.mutation(
  sendVoucherValueDecisionDrafts,
  [searchVoucherValueDecisionsQuery.prefix]
)

export const ignoreVoucherValueDecisionDraftsMutation = q.mutation(
  ignoreVoucherValueDecisionDrafts,
  [searchVoucherValueDecisionsQuery.prefix]
)

export const unignoreVoucherValueDecisionDraftsMutation = q.mutation(
  unignoreVoucherValueDecisionDrafts,
  [searchVoucherValueDecisionsQuery.prefix]
)
