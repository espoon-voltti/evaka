// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ApplicationDetails,
  ApplicationFormUpdate,
  deserializeApplicationDetails
} from 'lib-common/api-types/application/ApplicationDetails'
import { deserializePublicUnit } from 'lib-common/api-types/units/PublicUnit'
import {
  deserializeClubTerm,
  deserializePreschoolTerm
} from 'lib-common/api-types/units/terms'
import {
  ApplicationsOfChild,
  ApplicationType,
  CitizenApplicationUpdate,
  CitizenChildren
} from 'lib-common/generated/api-types/application'
import {
  ClubTerm,
  PreschoolTerm,
  PublicUnit
} from 'lib-common/generated/api-types/daycare'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

import { client } from '../api-client'

export type ApplicationUnitType =
  | 'CLUB'
  | 'DAYCARE'
  | 'PRESCHOOL'
  | 'PREPARATORY'

export function getApplicationUnits(
  type: ApplicationUnitType,
  date: LocalDate,
  shiftCare: boolean | null
): Promise<PublicUnit[]> {
  return client
    .get<JsonOf<PublicUnit[]>>('/public/units', {
      params: {
        type,
        date: date.formatIso(),
        ...(shiftCare && { shiftCare: shiftCare })
      }
    })
    .then((res) => res.data.map(deserializePublicUnit))
}

export function getApplication(
  applicationId: string
): Promise<ApplicationDetails> {
  return client
    .get<JsonOf<ApplicationDetails>>(`/citizen/applications/${applicationId}`)
    .then((res) => deserializeApplicationDetails(res.data))
}

export function updateApplication({
  applicationId,
  body
}: {
  applicationId: string
  body: CitizenApplicationUpdate
}): Promise<void> {
  return client
    .put(`/citizen/applications/${applicationId}`, body)
    .then(() => undefined)
}

export function saveApplicationDraft({
  applicationId,
  body
}: {
  applicationId: string
  body: ApplicationFormUpdate
}): Promise<void> {
  return client
    .put(`/citizen/applications/${applicationId}/draft`, body)
    .then(() => undefined)
}

export function removeUnprocessedApplication(
  applicationId: string
): Promise<void> {
  return client
    .delete(`/citizen/applications/${applicationId}`)
    .then(() => undefined)
}

export function sendApplication(applicationId: string): Promise<void> {
  return client
    .post(`/citizen/applications/${applicationId}/actions/send-application`)
    .then(() => undefined)
}

export function getGuardianApplications(): Promise<ApplicationsOfChild[]> {
  return client
    .get<JsonOf<ApplicationsOfChild[]>>('/citizen/applications/by-guardian')
    .then((res) => res.data.map(deserializeApplicationsOfChild))
}

const deserializeApplicationsOfChild = (
  json: JsonOf<ApplicationsOfChild>
): ApplicationsOfChild => ({
  ...json,
  applicationSummaries: json.applicationSummaries.map((json2) => ({
    ...json2,
    sentDate: LocalDate.parseNullableIso(json2.sentDate),
    startDate: LocalDate.parseNullableIso(json2.startDate),
    createdDate: HelsinkiDateTime.parseIso(json2.createdDate),
    modifiedDate: HelsinkiDateTime.parseIso(json2.modifiedDate)
  }))
})

export const getApplicationChildren = (): Promise<CitizenChildren[]> =>
  client
    .get<JsonOf<CitizenChildren[]>>('/citizen/applications/children')
    .then(({ data }) =>
      data.map((child) => ({
        ...child,
        dateOfBirth: LocalDate.parseIso(child.dateOfBirth)
      }))
    )

export function createApplication({
  childId,
  type
}: {
  childId: string
  type: ApplicationType
}): Promise<string> {
  return client
    .post<JsonOf<string>>('/citizen/applications', {
      childId,
      type: type.toUpperCase()
    })
    .then((res) => res.data)
}

export function getDuplicateApplications(
  childId: string
): Promise<Record<ApplicationType, boolean>> {
  return client
    .get<
      JsonOf<Record<ApplicationType, boolean>>
    >(`/citizen/applications/duplicates/${childId}`)
    .then((res) => res.data)
}

export function getActivePlacementsByApplicationType(
  childId: string
): Promise<Record<ApplicationType, boolean>> {
  return client
    .get<
      Record<ApplicationType, boolean>
    >(`/citizen/applications/active-placements/${childId}`)
    .then((res) => res.data)
}

export function getClubTerms(): Promise<ClubTerm[]> {
  return client
    .get<JsonOf<ClubTerm[]>>(`/public/club-terms`)
    .then((res) => res.data.map(deserializeClubTerm))
}

export function getPreschoolTerms(): Promise<PreschoolTerm[]> {
  return client
    .get<JsonOf<PreschoolTerm[]>>(`/public/preschool-terms`)
    .then((res) => res.data.map(deserializePreschoolTerm))
}

export function getServiceNeedOptionPublicInfos(
  placementTypes: PlacementType[]
): Promise<ServiceNeedOptionPublicInfo[]> {
  return client
    .get<
      JsonOf<ServiceNeedOptionPublicInfo[]>
    >('/public/service-needs/options', { params: { placementTypes: placementTypes.join() } })
    .then((res) => res.data)
}
