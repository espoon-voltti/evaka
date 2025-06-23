// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import {
  acceptDecision,
  createNote,
  deleteNote,
  getApplicationDetails,
  getNotes,
  rejectDecision,
  sendApplication,
  setApplicationVerified,
  updateAndSendApplication,
  updateApplication,
  updateNote
} from '../../generated/api-clients/application'
import { getApplicationMetadata } from '../../generated/api-clients/caseprocess'
import {
  getApplicationUnits,
  getClubTerms,
  getPreschoolTerms
} from '../../generated/api-clients/daycare'
import { getThreadByApplicationId } from '../../generated/api-clients/messaging'

const q = new Queries()

export const clubTermsQuery = q.query(getClubTerms)

export const preschoolTermsQuery = q.query(getPreschoolTerms)

export const applicationDetailsQuery = q.query(getApplicationDetails)

export const applicationMetadataQuery = q.query(getApplicationMetadata)

export const updateApplicationMutation = q.mutation(updateApplication, [
  ({ applicationId }) => applicationDetailsQuery({ applicationId }),
  ({ applicationId }) => applicationMetadataQuery({ applicationId })
])

export const sendApplicationMutation = q.mutation(sendApplication, [
  ({ applicationId }) => applicationDetailsQuery({ applicationId }),
  ({ applicationId }) => applicationMetadataQuery({ applicationId })
])

export const updateAndSendApplicationMutation = q.mutation(
  updateAndSendApplication,
  [
    ({ applicationId }) => applicationDetailsQuery({ applicationId }),
    ({ applicationId }) => applicationMetadataQuery({ applicationId })
  ]
)

export const setApplicationVerifiedMutation = q.mutation(
  setApplicationVerified,
  [
    ({ applicationId }) => applicationDetailsQuery({ applicationId }),
    ({ applicationId }) => applicationMetadataQuery({ applicationId })
  ]
)

export const acceptDecisionMutation = q.mutation(acceptDecision, [
  ({ applicationId }) => applicationDetailsQuery({ applicationId }),
  ({ applicationId }) => applicationMetadataQuery({ applicationId })
])

export const rejectDecisionMutation = q.mutation(rejectDecision, [
  ({ applicationId }) => applicationDetailsQuery({ applicationId }),
  ({ applicationId }) => applicationMetadataQuery({ applicationId })
])

export const applicationUnitsQuery = q.query(getApplicationUnits)

export const threadByApplicationIdQuery = q.query(getThreadByApplicationId)

export const applicationNotesQuery = q.query(getNotes)

export const createApplicationNoteMutation = q.mutation(createNote, [
  ({ applicationId }) => applicationNotesQuery({ applicationId })
])

export const updateApplicationNoteMutation = q.parametricMutation<{
  applicationId: ApplicationId
}>()(updateNote, [
  ({ applicationId }) => applicationNotesQuery({ applicationId })
])

export const deleteApplicationNoteMutation = q.parametricMutation<{
  applicationId: ApplicationId
}>()(deleteNote, [
  ({ applicationId }) => applicationNotesQuery({ applicationId })
])
