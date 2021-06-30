// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  ClubTerm,
  deserializeClubTerm,
  deserializePreschoolTerm,
  PreschoolTerm
} from 'lib-common/api-types/units/terms'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import {
  ApplicationDetails,
  ApplicationFormUpdate,
  deserializeApplicationDetails
} from 'lib-common/api-types/application/ApplicationDetails'
import {
  ApplicationsOfChild,
  deserializeApplicationsOfChild
} from 'lib-common/api-types/application/ApplicationsOfChild'
import {
  ApplicationType,
  AttachmentType
} from 'lib-common/api-types/application/enums'
import { client } from '../api-client'
import {
  deserializePublicUnit,
  PublicUnit
} from 'lib-common/api-types/units/PublicUnit'
import { UUID } from 'lib-common/types'
import {
  PlacementType,
  ServiceNeedOptionPublicInfo
} from 'lib-common/api-types/serviceNeed/common'

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

export async function saveAttachment(
  applicationId: UUID,
  file: File,
  attachmentType: AttachmentType,
  onUploadProgress: (progressEvent: ProgressEvent) => void
): Promise<Result<UUID>> {
  const formData = new FormData()
  formData.append('file', file)

  try {
    interface AttachmentResult {
      data: string
    }
    const { data }: AttachmentResult = await client.post(
      `/attachments/citizen/applications/${applicationId}?type=${attachmentType}`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress
      }
    )
    return Success.of(data)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function deleteAttachment(id: UUID): Promise<Result<void>> {
  try {
    await client.delete(`/attachments/citizen/${id}`)
    return Success.of(void 0)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function getAttachmentBlob(
  attachmentId: UUID
): Promise<Result<BlobPart>> {
  try {
    const result = await client({
      url: `/attachments/${attachmentId}/download`,
      method: 'GET',
      responseType: 'blob'
    })
    return Success.of(result.data)
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
