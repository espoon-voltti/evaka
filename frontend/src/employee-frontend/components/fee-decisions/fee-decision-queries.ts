// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  confirmFeeDecisionDrafts,
  ignoreFeeDecisionDrafts,
  searchFeeDecisions,
  unignoreFeeDecisionDrafts
} from '../../generated/api-clients/invoicing'
import { getFeeDecisionMetadata } from '../../generated/api-clients/process'

const q = new Queries()

export const searchFeeDecisionsQuery = q.query(searchFeeDecisions)

export const feeDecisionMetadataQuery = q.query(getFeeDecisionMetadata)

export const confirmFeeDecisionDraftsMutation = q.mutation(
  confirmFeeDecisionDrafts,
  [searchFeeDecisionsQuery.prefix]
)

export const ignoreFeeDecisionDraftsMutation = q.mutation(
  ignoreFeeDecisionDrafts,
  [searchFeeDecisionsQuery.prefix]
)

export const unignoreFeeDecisionDraftsMutation = q.mutation(
  unignoreFeeDecisionDrafts,
  [searchFeeDecisionsQuery.prefix]
)
