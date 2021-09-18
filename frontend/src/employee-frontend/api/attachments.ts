// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { client } from './client'
import { AttachmentType } from 'lib-common/generated/enums'

async function doSaveAttachment(
  config: { path: string; params?: unknown },
  file: File,
  onUploadProgress: (progressEvent: ProgressEvent) => void
): Promise<Result<UUID>> {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const { data } = await client.post<UUID>(config.path, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      params: config.params,
      onUploadProgress
    })
    return Success.of(data)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function saveApplicationAttachment(
  applicationId: UUID,
  file: File,
  type: AttachmentType,
  onUploadProgress: (progressEvent: ProgressEvent) => void
): Promise<Result<UUID>> {
  return await doSaveAttachment(
    { path: `/attachments/applications/${applicationId}`, params: { type } },
    file,
    onUploadProgress
  )
}

export async function saveIncomeStatementAttachment(
  incomeStatementId: UUID,
  file: File,
  onUploadProgress: (progressEvent: ProgressEvent) => void
): Promise<Result<UUID>> {
  return await doSaveAttachment(
    { path: `/attachments/income-statements/${incomeStatementId}` },
    file,
    onUploadProgress
  )
}

export const saveMessageAttachment = (
  draftId: UUID,
  file: File,
  onUploadProgress: (progressEvent: ProgressEvent) => void
): Promise<Result<UUID>> =>
  doSaveAttachment(
    { path: `/attachments/messages/${draftId}` },
    file,
    onUploadProgress
  )

export const deleteAttachment = (id: UUID): Promise<Result<void>> =>
  client
    .delete(`/attachments/${id}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))

export async function getAttachmentBlob(
  attachmentId: UUID
): Promise<Result<BlobPart>> {
  return client({
    url: `/attachments/${attachmentId}/download`,
    method: 'GET',
    responseType: 'blob'
  })
    .then((result) => Success.of(result.data))
    .catch((e) => Failure.fromError(e))
}
