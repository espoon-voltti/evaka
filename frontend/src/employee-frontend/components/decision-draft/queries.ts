// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  getDecisionDrafts,
  updateDecisionDrafts
} from '../../generated/api-clients/application'
import {
  getDecisionUnits,
  getDraftReasoningPreview,
  getIndividualReasonings,
  linkIndividualReasoning,
  unlinkIndividualReasoning
} from '../../generated/api-clients/decision'

const q = new Queries()

export const decisionDraftsQuery = q.query(getDecisionDrafts)
export const decisionUnitsQuery = q.query(getDecisionUnits)
export const updateDecisionDraftsMutation = q.mutation(updateDecisionDrafts, [
  decisionDraftsQuery.prefix
])

export const draftReasoningPreviewQuery = q.query(getDraftReasoningPreview)

export const eligibleIndividualReasoningsQuery = q.query(
  getIndividualReasonings
)

export const linkIndividualReasoningMutation = q.mutation(
  linkIndividualReasoning,
  [({ id }) => draftReasoningPreviewQuery({ id })]
)

export const unlinkIndividualReasoningMutation = q.mutation(
  unlinkIndividualReasoning,
  [({ id }) => draftReasoningPreviewQuery({ id })]
)
