// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationType } from 'lib-common/generated/api-types/application'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../query'

import {
  ApplicationUnitType,
  createApplication,
  getActivePlacementsByApplicationType,
  getApplication,
  getApplicationChildren,
  getApplicationUnits,
  getClubTerms,
  getDuplicateApplications,
  getGuardianApplications,
  getPreschoolTerms,
  getServiceNeedOptionPublicInfos,
  removeUnprocessedApplication,
  saveApplicationDraft,
  sendApplication,
  updateApplication
} from './api'

const queryKeys = createQueryKeys('applications', {
  units: (
    applicationType: ApplicationType,
    preparatory: boolean,
    preferredStartDate: LocalDate | null,
    shiftCare: boolean | null
  ) => [
    'units',
    {
      applicationType,
      preparatory,
      preferredStartDate: preferredStartDate?.formatIso(),
      shiftCare
    }
  ],
  application: (applicationId: UUID) => ['application', applicationId],
  guardianApplications: () => ['guardianApplications'],
  children: () => ['children'],
  duplicates: (childId: UUID) => ['duplicates', childId],
  activePlacementsByApplicationType: (childId: UUID) => [
    'activePlacementsByApplicationType',
    childId
  ],
  preschoolTerms: () => ['preschoolTerms'],
  clubTerms: () => ['clubTerms'],
  serviceNeedOptionPublicInfos: (placementTypes: PlacementType[]) => [
    'serviceNeedOptionPublicInfos',
    placementTypes
  ]
})

export const applicationUnitsQuery = query({
  api: (
    applicationType: ApplicationType,
    preparatory: boolean,
    preferredStartDate: LocalDate | null,
    shiftCare: boolean | null
  ) => {
    const unitType: ApplicationUnitType =
      applicationType === 'CLUB'
        ? 'CLUB'
        : applicationType === 'DAYCARE'
        ? 'DAYCARE'
        : preparatory
        ? 'PREPARATORY'
        : 'PRESCHOOL'
    return preferredStartDate
      ? getApplicationUnits(unitType, preferredStartDate, shiftCare)
      : Promise.resolve([])
  },
  queryKey: queryKeys.units
})

export const applicationQuery = query({
  api: getApplication,
  queryKey: queryKeys.application
})

export const guardianApplicationsQuery = query({
  api: getGuardianApplications,
  queryKey: queryKeys.guardianApplications
})

export const applicationChildrenQuery = query({
  api: getApplicationChildren,
  queryKey: queryKeys.children
})

export const duplicateApplicationsQuery = query({
  api: getDuplicateApplications,
  queryKey: queryKeys.duplicates
})

export const activePlacementsByApplicationTypeQuery = query({
  api: getActivePlacementsByApplicationType,
  queryKey: queryKeys.activePlacementsByApplicationType
})

export const preschoolTermsQuery = query({
  api: getPreschoolTerms,
  queryKey: queryKeys.preschoolTerms
})

export const clubTermsQuery = query({
  api: getClubTerms,
  queryKey: queryKeys.clubTerms
})

export const serviceNeedOptionPublicInfosQuery = query({
  api: getServiceNeedOptionPublicInfos,
  queryKey: queryKeys.serviceNeedOptionPublicInfos
})

export const createApplicationMutation = mutation({
  api: createApplication
})

export const updateApplicationMutation = mutation({
  api: updateApplication,
  invalidateQueryKeys: ({ applicationId }) => [
    applicationQuery(applicationId).queryKey
  ]
})

export const saveApplicationDraftMutation = mutation({
  api: saveApplicationDraft,
  invalidateQueryKeys: ({ applicationId }) => [
    applicationQuery(applicationId).queryKey
  ]
})

export const removeUnprocessableApplicationMutation = mutation({
  api: removeUnprocessedApplication,
  invalidateQueryKeys: () => [guardianApplicationsQuery().queryKey]
})

export const sendApplicationMutation = mutation({
  api: sendApplication,
  invalidateQueryKeys: (applicationId) => [
    applicationQuery(applicationId).queryKey
  ]
})
