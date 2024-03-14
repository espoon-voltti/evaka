// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { AttachmentType } from 'lib-common/generated/api-types/attachment'
import { UUID } from 'lib-common/types'

import { API_URL, client } from './api-client'

async function doSaveAttachment(
  url: string,
  file: File,
  onUploadProgress: (percentage: number) => void
): Promise<Result<UUID>> {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const { data } = await client.post<string>(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: ({ loaded, total }) =>
        onUploadProgress(
          total !== undefined && total !== 0
            ? Math.round((loaded / total) * 100)
            : 0
        )
    })
    return Success.of(data)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function saveIncomeStatementAttachment(
  incomeStatementId: UUID | undefined,
  file: File,
  onUploadProgress: (percentage: number) => void
): Promise<Result<UUID>> {
  return doSaveAttachment(
    incomeStatementId
      ? `/citizen/attachments/income-statements/${incomeStatementId}`
      : '/citizen/attachments/income-statements',
    file,
    onUploadProgress
  )
}

export async function saveApplicationAttachment(
  applicationId: UUID,
  file: File,
  attachmentType: AttachmentType,
  onUploadProgress: (percentage: number) => void
): Promise<Result<UUID>> {
  return doSaveAttachment(
    `/citizen/attachments/applications/${applicationId}?type=${attachmentType}`,
    file,
    onUploadProgress
  )
}

export function getAttachmentUrl(
  attachmentId: UUID,
  requestedFilename: string
): string {
  const encodedFilename = encodeURIComponent(requestedFilename)
  return `${API_URL}/citizen/attachments/${attachmentId}/download/${encodedFilename}`
}
