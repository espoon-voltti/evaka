// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import type {
  ApplicationDetails,
  ApplicationFormUpdate
} from 'lib-common/api-types/application/ApplicationDetails'
import { deserializeApplicationDetails } from 'lib-common/api-types/application/ApplicationDetails'
import { deserializePublicUnit } from 'lib-common/api-types/units/PublicUnit'
import {
  deserializeClubTerm,
  deserializePreschoolTerm
} from 'lib-common/api-types/units/terms'
import type {
  ApplicationsOfChild,
  ApplicationType
} from 'lib-common/generated/api-types/application'
import type {
  ClubTerm,
  PreschoolTerm,
  PublicUnit
} from 'lib-common/generated/api-types/daycare'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

import { client } from '../api-client'

export type ApplicationUnitType =
  | 'CLUB'
  | 'DAYCARE'
  | 'PRESCHOOL'
  | 'PREPARATORY'

export async function getApplicationUnits(
  type: ApplicationUnitType,
  date: LocalDate,
  shiftCare: boolean | null
): Promise<Result<PublicUnit[]>> {
  try {
    const { data } = await client.get<JsonOf<PublicUnit[]>>('/units', {
      params: {
        type,
        date: date.formatIso(),
        ...(shiftCare && { shiftCare: shiftCare })
      }
    })
    return Success.of(data.map(deserializePublicUnit))
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function getApplication(
  applicationId: string
): Promise<Result<ApplicationDetails>> {
  try {
    const { data } = await client.get<JsonOf<ApplicationDetails>>(
      `/citizen/applications/${applicationId}`
    )
    return Success.of(deserializeApplicationDetails(data))
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function updateApplication(
  applicationId: string,
  data: ApplicationFormUpdate
): Promise<Result<void>> {
  try {
    await client.put(`/citizen/applications/${applicationId}`, data)
    return Success.of(undefined)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function saveApplicationDraft(
  applicationId: string,
  data: ApplicationFormUpdate
): Promise<Result<void>> {
  try {
    await client.put(`/citizen/applications/${applicationId}/draft`, data)
    return Success.of(undefined)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function removeUnprocessedApplication(
  applicationId: string
): Promise<Result<void>> {
  try {
    await client.delete(`/citizen/applications/${applicationId}`)
    return Success.of(undefined)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function sendApplication(
  applicationId: string
): Promise<Result<void>> {
  try {
    await client.post(
      `/citizen/applications/${applicationId}/actions/send-application`
    )
    return Success.of(undefined)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export const getGuardianApplications = async (): Promise<
  Result<ApplicationsOfChild[]>
> => {
  return client
    .get<JsonOf<ApplicationsOfChild[]>>('/citizen/applications/by-guardian')
    .then((res) => res.data.map(deserializeApplicationsOfChild))
    .then((data) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

const deserializeApplicationsOfChild = (
  json: JsonOf<ApplicationsOfChild>
): ApplicationsOfChild => ({
  ...json,
  applicationSummaries: json.applicationSummaries.map((json2) => ({
    ...json2,
    sentDate: LocalDate.parseNullableIso(json2.sentDate),
    startDate: LocalDate.parseNullableIso(json2.startDate),
    createdDate: new Date(json2.createdDate),
    modifiedDate: new Date(json2.modifiedDate)
  }))
})

export async function createApplication(
  childId: string,
  type: ApplicationType
): Promise<Result<string>> {
  try {
    const { data: applicationId } = await client.post<string>(
      '/citizen/applications',
      {
        childId,
        type: type.toUpperCase()
      }
    )
    return Success.of(applicationId)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function getDuplicateApplications(
  childId: string
): Promise<Result<Record<ApplicationType, boolean>>> {
  try {
    const { data } = await client.get<Record<ApplicationType, boolean>>(
      `/citizen/applications/duplicates/${childId}`
    )
    return Success.of(data)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function getActivePlacementsByApplicationType(
  childId: string
): Promise<Result<Record<ApplicationType, boolean>>> {
  try {
    const { data } = await client.get<Record<ApplicationType, boolean>>(
      `/citizen/applications/active-placements/${childId}`
    )
    return Success.of(data)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function getClubTerms(): Promise<Result<ClubTerm[]>> {
  try {
    const result = await client.get<JsonOf<ClubTerm[]>>(`/public/club-terms`)
    return Success.of(result.data.map(deserializeClubTerm))
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function getPreschoolTerms(): Promise<Result<PreschoolTerm[]>> {
  try {
    const result = await client.get<JsonOf<PreschoolTerm[]>>(
      `/public/preschool-terms`
    )
    return Success.of(result.data.map(deserializePreschoolTerm))
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function getServiceNeedOptionPublicInfos(
  placementTypes: PlacementType[]
): Promise<Result<ServiceNeedOptionPublicInfo[]>> {
  return client
    .get<JsonOf<ServiceNeedOptionPublicInfo[]>>(
      '/public/service-needs/options',
      { params: { placementTypes: placementTypes.join() } }
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
