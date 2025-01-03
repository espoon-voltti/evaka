// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { AttachmentType } from 'lib-common/generated/api-types/attachment'
import {
  ApplicationId,
  AttachmentId,
  IncomeStatementId
} from 'lib-common/generated/api-types/shared'

import { API_URL, client } from './api-client'

async function doSaveAttachment(
  url: string,
  file: File,
  onUploadProgress: (percentage: number) => void
): Promise<Result<AttachmentId>> {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const { data } = await client.post<AttachmentId>(url, formData, {
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
  incomeStatementId: IncomeStatementId | undefined,
  file: File,
  onUploadProgress: (percentage: number) => void
): Promise<Result<AttachmentId>> {
  return doSaveAttachment(
    incomeStatementId
      ? `/citizen/attachments/income-statements/${incomeStatementId}`
      : '/citizen/attachments/income-statements',
    file,
    onUploadProgress
  )
}

export async function saveMessageAttachment(
  file: File,
  onUploadProgress: (percentage: number) => void
): Promise<Result<AttachmentId>> {
  return doSaveAttachment(
    '/citizen/attachments/messages',
    file,
    onUploadProgress
  )
}

export async function saveApplicationAttachment(
  applicationId: ApplicationId,
  file: File,
  attachmentType: AttachmentType,
  onUploadProgress: (percentage: number) => void
): Promise<Result<AttachmentId>> {
  return doSaveAttachment(
    `/citizen/attachments/applications/${applicationId}?type=${attachmentType}`,
    file,
    onUploadProgress
  )
}

export function getAttachmentUrl(
  attachmentId: AttachmentId,
  requestedFilename: string
): string {
  const encodedFilename = encodeURIComponent(requestedFilename)
  return `${API_URL}/citizen/attachments/${attachmentId}/download/${encodedFilename}`
}
