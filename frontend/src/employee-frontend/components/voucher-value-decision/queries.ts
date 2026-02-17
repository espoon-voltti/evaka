// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  getVoucherValueDecision,
  markVoucherValueDecisionSent,
  sendVoucherValueDecisionDrafts,
  setVoucherValueDecisionType
} from '../../generated/api-clients/invoicing'

const q = new Queries()

export const voucherValueDecisionQuery = q.query(getVoucherValueDecision)

export const setVoucherValueDecisionTypeMutation = q.mutation(
  setVoucherValueDecisionType,
  [voucherValueDecisionQuery.prefix]
)

export const sendVoucherValueDecisionDraftsMutation = q.mutation(
  sendVoucherValueDecisionDrafts,
  [voucherValueDecisionQuery.prefix]
)

export const markVoucherValueDecisionSentMutation = q.mutation(
  markVoucherValueDecisionSent,
  [voucherValueDecisionQuery.prefix]
)
