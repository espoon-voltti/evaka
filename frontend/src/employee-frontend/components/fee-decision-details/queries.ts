// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  confirmFeeDecisionDrafts,
  getFeeDecision,
  setFeeDecisionSent,
  setFeeDecisionType
} from '../../generated/api-clients/invoicing'

const q = new Queries()

export const feeDecisionQuery = q.query(getFeeDecision)

export const setFeeDecisionTypeMutation = q.mutation(setFeeDecisionType, [
  feeDecisionQuery.prefix
])

export const confirmFeeDecisionDraftsMutation = q.mutation(
  confirmFeeDecisionDrafts,
  [feeDecisionQuery.prefix]
)

export const setFeeDecisionSentMutation = q.mutation(setFeeDecisionSent, [
  feeDecisionQuery.prefix
])
