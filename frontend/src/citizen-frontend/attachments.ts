// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import type { AttachmentType } from 'lib-common/generated/api-types/attachment'
import type { UUID } from 'lib-common/types'

import { API_URL, client } from './api-client'

async function doSaveAttachment(
  url: string,
  file: File,
  onUploadProgress: (progressEvent: ProgressEvent) => void
): Promise<Result<UUID>> {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const { data } = await client.post<string>(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress
    })
    return Success.of(data)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function saveIncomeStatementAttachment(
  incomeStatementId: UUID | undefined,
  file: File,
  onUploadProgress: (progressEvent: ProgressEvent) => void
): Promise<Result<UUID>> {
  return doSaveAttachment(
    `/attachments/citizen/income-statements/${incomeStatementId ?? ''}`,
    file,
    onUploadProgress
  )
}

export async function saveApplicationAttachment(
  applicationId: UUID,
  file: File,
  attachmentType: AttachmentType,
  onUploadProgress: (progressEvent: ProgressEvent) => void
): Promise<Result<UUID>> {
  return doSaveAttachment(
    `/attachments/citizen/applications/${applicationId}?type=${attachmentType}`,
    file,
    onUploadProgress
  )
}

export async function deleteAttachment(id: UUID): Promise<Result<void>> {
  try {
    await client.delete(`/attachments/citizen/${id}`)
    return Success.of(void 0)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export function getAttachmentUrl(
  attachmentId: UUID,
  requestedFilename: string
): string {
  return `${API_URL}/attachments/${attachmentId}/download/${requestedFilename}`
}
