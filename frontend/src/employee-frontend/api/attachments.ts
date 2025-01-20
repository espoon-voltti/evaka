// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Success } from 'lib-common/api'
import { AttachmentType } from 'lib-common/generated/api-types/attachment'
import { AttachmentId } from 'lib-common/generated/api-types/shared'
import { UUID } from 'lib-common/types'
import { UploadHandler } from 'lib-components/molecules/FileUpload'

import { API_URL, client } from './client'

function uploadHandler(config: {
  path: string
  params?: unknown
}): UploadHandler {
  return async (file, onUploadProgress) => {
    const formData = new FormData()
    formData.append('file', file)

    try {
      const { data } = await client.post<AttachmentId>(config.path, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        params: config.params,
        onUploadProgress: ({ loaded, total }) =>
          onUploadProgress(
            total !== undefined && total !== 0
              ? Math.round((loaded * 100) / total)
              : 0
          )
      })
      return Success.of(data)
    } catch (e) {
      return Failure.fromError(e)
    }
  }
}

export function saveApplicationAttachment(
  applicationId: UUID,
  type: AttachmentType
): UploadHandler {
  return uploadHandler({
    path: `/employee/attachments/applications/${applicationId}`,
    params: { type }
  })
}

export function saveIncomeStatementAttachment(
  incomeStatementId: UUID
): UploadHandler {
  return uploadHandler({
    path: `/employee/attachments/income-statements/${incomeStatementId}`
  })
}

export function saveIncomeAttachment(incomeId: UUID | null): UploadHandler {
  return uploadHandler({
    path: incomeId
      ? `/employee/attachments/income/${incomeId}`
      : `/employee/attachments/income`
  })
}

export function saveInvoiceAttachment(invoiceId: UUID): UploadHandler {
  return uploadHandler({ path: `/employee/attachments/invoices/${invoiceId}` })
}

export function saveFeeAlterationAttachment(
  feeAlterationId: UUID | null
): UploadHandler {
  return uploadHandler({
    path: feeAlterationId
      ? `/employee/attachments/fee-alteration/${feeAlterationId}`
      : `/employee/attachments/fee-alteration`
  })
}

export function saveMessageAttachment(draftId: UUID): UploadHandler {
  return uploadHandler({ path: `/employee/attachments/messages/${draftId}` })
}

export function savePedagogicalDocumentAttachment(
  documentId: UUID
): UploadHandler {
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
