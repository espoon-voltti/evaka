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
  linkIndividualReasonings
} from '../../generated/api-clients/decision'

const q = new Queries()

export const decisionDraftsQuery = q.query(getDecisionDrafts)
export const decisionUnitsQuery = q.query(getDecisionUnits)
export const updateDecisionDraftsMutation = q.mutation(updateDecisionDrafts, [
  decisionDraftsQuery.prefix
])

export const getDraftReasoningPreviewQuery = q.query(getDraftReasoningPreview)

export const getIndividualReasoningsQuery = q.query(getIndividualReasonings)

export const linkIndividualReasoningsMutation = q.mutation(
  linkIndividualReasonings,
  [({ id }) => getDraftReasoningPreviewQuery({ id })]
)
