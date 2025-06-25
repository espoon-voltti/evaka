// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  createApplication,
  deleteOrCancelUnprocessedApplication,
  getApplication,
  getApplicationChildren,
  getChildDuplicateApplications,
  getChildPlacementStatusByApplicationType,
  getGuardianApplications,
  saveApplicationAsDraft,
  sendApplication,
  updateApplication
} from '../generated/api-clients/application'
import { getApplicationMetadata } from '../generated/api-clients/caseprocess'
import {
  getApplicationUnits,
  getClubTerms,
  getPreschoolTerms
} from '../generated/api-clients/daycare'
import { getServiceNeedOptionPublicInfos } from '../generated/api-clients/serviceneed'

const q = new Queries()

export const applicationUnitsQuery = q.query(getApplicationUnits)

export const applicationQuery = q.query(getApplication)

export const guardianApplicationsQuery = q.query(getGuardianApplications)

export const applicationChildrenQuery = q.query(getApplicationChildren)

export const duplicateApplicationsQuery = q.query(getChildDuplicateApplications)

export const activePlacementsByApplicationTypeQuery = q.query(
  getChildPlacementStatusByApplicationType
)

export const preschoolTermsQuery = q.query(getPreschoolTerms)

export const clubTermsQuery = q.query(getClubTerms)

export const serviceNeedOptionPublicInfosQuery = q.query(
  getServiceNeedOptionPublicInfos
)

export const createApplicationMutation = q.mutation(createApplication)

export const updateApplicationMutation = q.mutation(updateApplication, [
  ({ applicationId }) => applicationQuery({ applicationId })
])

export const saveApplicationDraftMutation = q.mutation(saveApplicationAsDraft, [
  ({ applicationId }) => applicationQuery({ applicationId })
])

export const removeUnprocessableApplicationMutation = q.mutation(
  deleteOrCancelUnprocessedApplication,
  [guardianApplicationsQuery]
)

export const sendApplicationMutation = q.mutation(sendApplication, [
  (applicationId) => applicationQuery(applicationId)
])

export const applicationMetadataQuery = q.query(getApplicationMetadata)
