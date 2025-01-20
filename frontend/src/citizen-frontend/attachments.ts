// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Success, wrapResult } from 'lib-common/api'
import { ApplicationAttachmentType } from 'lib-common/generated/api-types/application'
import {
  ApplicationId,
  AttachmentId,
  IncomeStatementId
} from 'lib-common/generated/api-types/shared'
import { UploadHandler } from 'lib-components/molecules/FileUpload'

import { API_URL, client } from './api-client'
import { deleteAttachment } from './generated/api-clients/attachment'

function uploadHandler(url: string): UploadHandler {
  return {
    upload: async (file, onUploadProgress) => {
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
    },
    delete: deleteAttachmentResult
  }
}

const deleteAttachmentResult = wrapResult(deleteAttachment)

export function incomeStatementAttachment(
  incomeStatementId: IncomeStatementId | undefined
): UploadHandler {
  return uploadHandler(
    incomeStatementId
      ? `/citizen/attachments/income-statements/${incomeStatementId}`
      : '/citizen/attachments/income-statements'
  )
}

export const messageAttachment = uploadHandler('/citizen/attachments/messages')

export function applicationAttachment(
  applicationId: ApplicationId,
  attachmentType: ApplicationAttachmentType
): UploadHandler {
  return uploadHandler(
    `/citizen/attachments/applications/${applicationId}?type=${attachmentType}`
  )
}

export function getAttachmentUrl(
  attachmentId: AttachmentId,
  requestedFilename: string
): string {
  const encodedFilename = encodeURIComponent(requestedFilename)
  return `${API_URL}/citizen/attachments/${attachmentId}/download/${encodedFilename}`
}
