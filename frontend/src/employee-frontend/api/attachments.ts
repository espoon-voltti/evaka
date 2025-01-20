// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Success, wrapResult } from 'lib-common/api'
import { ApplicationAttachmentType } from 'lib-common/generated/api-types/application'
import { AttachmentId } from 'lib-common/generated/api-types/shared'
import { UUID } from 'lib-common/types'
import { UploadHandler } from 'lib-components/molecules/FileUpload'

import { deleteAttachment } from '../generated/api-clients/attachment'

import { API_URL, client } from './client'

function uploadHandler(config: {
  path: string
  params?: unknown
}): UploadHandler {
  return {
    upload: async (file, onUploadProgress) => {
      const formData = new FormData()
      formData.append('file', file)

      try {
        const { data } = await client.post<AttachmentId>(
          config.path,
          formData,
          {
            headers: { 'Content-Type': 'multipart/form-data' },
            params: config.params,
            onUploadProgress: ({ loaded, total }) =>
              onUploadProgress(
                total !== undefined && total !== 0
                  ? Math.round((loaded * 100) / total)
                  : 0
              )
          }
        )
        return Success.of(data)
      } catch (e) {
        return Failure.fromError(e)
      }
    },
    delete: deleteAttachmentResult
  }
}

const deleteAttachmentResult = wrapResult(deleteAttachment)

export function applicationAttachment(
  applicationId: UUID,
  type: ApplicationAttachmentType
): UploadHandler {
  return uploadHandler({
    path: `/employee/attachments/applications/${applicationId}`,
    params: { type }
  })
}

export function incomeStatementAttachment(
  incomeStatementId: UUID
): UploadHandler {
  return uploadHandler({
    path: `/employee/attachments/income-statements/${incomeStatementId}`
  })
}

export function incomeAttachment(incomeId: UUID | null): UploadHandler {
  return uploadHandler({
    path: incomeId
      ? `/employee/attachments/income/${incomeId}`
      : `/employee/attachments/income`
  })
}

export function invoiceAttachment(invoiceId: UUID): UploadHandler {
  return uploadHandler({ path: `/employee/attachments/invoices/${invoiceId}` })
}

export function feeAlterationAttachment(
  feeAlterationId: UUID | null
): UploadHandler {
  return uploadHandler({
    path: feeAlterationId
      ? `/employee/attachments/fee-alteration/${feeAlterationId}`
      : `/employee/attachments/fee-alteration`
  })
}

export function messageAttachment(draftId: UUID): UploadHandler {
  return uploadHandler({ path: `/employee/attachments/messages/${draftId}` })
}

export function pedagogicalDocumentAttachment(documentId: UUID): UploadHandler {
  return uploadHandler({
    path: `/employee/attachments/pedagogical-documents/${documentId}`
  })
}

export function getAttachmentUrl(
  attachmentId: UUID,
  requestedFilename: string
): string {
  const encodedFilename = encodeURIComponent(requestedFilename)
  return `${API_URL}/employee/attachments/${attachmentId}/download/${encodedFilename}`
}
