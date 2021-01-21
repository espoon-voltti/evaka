// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from '@evaka/lib-common/src/api'
import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'
import {
  ApplicationDetails,
  ApplicationFormUpdate,
  deserializeApplicationDetails
} from '@evaka/lib-common/src/api-types/application/ApplicationDetails'
import {
  ApplicationsOfChild,
  deserializeApplicationsOfChild
} from '@evaka/lib-common/src/api-types/application/ApplicationsOfChild'
import { ApplicationType, AttachmentType } from '@evaka/lib-common/src/api-types/application/enums'
import { client } from '~api-client'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units/PublicUnit'
import { UUID } from '~../../lib-common/src/types'

export type ApplicationUnitType =
  | 'CLUB'
  | 'DAYCARE'
  | 'PRESCHOOL'
  | 'PREPARATORY'

export async function getApplicationUnits(
  type: ApplicationUnitType,
  date: LocalDate
): Promise<Result<PublicUnit[]>> {
  try {
    const { data } = await client.get<JsonOf<PublicUnit[]>>('/units', {
      params: {
        type,
        date: date.formatIso()
      }
    })
    return Success.of(data)
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
): Promise<string> {
  const { data: applicationId } = await client.post<string>(
    '/citizen/applications',
    {
      childId,
      type: type.toUpperCase() // FIXME
    }
  )
  return applicationId
}

export async function getDuplicateApplications(
  childId: string
): Promise<Record<ApplicationType, boolean>> {
  const { data } = await client.get<Record<ApplicationType, boolean>>(
    `/citizen/applications/duplicates/${childId}`
  )
  return Object.fromEntries(
    Object.entries(data).map(([type, value]) => [type.toLowerCase(), value])
  ) as Record<ApplicationType, boolean> // FIXME
}

export async function saveAttachment(
  applicationId: UUID,
  file: File,
  attachmentType: AttachmentType,
  onUploadProgress: (progressEvent: any) => void
): Promise<Result<UUID>> {
  const formData = new FormData()
  formData.append('file', file)

  try {
    interface AttachmentResult {
      data: string
    }
    const { data }: AttachmentResult = await client.post(
      `/attachments/enduser/applications/${applicationId}?type=${attachmentType}`,
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
    await client.delete(`/attachments/enduser/${id}`)
    return Success.of(void 0)
  } catch (e) {
    return Failure.fromError(e)
  }
}
