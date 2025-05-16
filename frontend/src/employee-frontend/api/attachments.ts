// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { AxiosProgressEvent } from 'axios'

import { Failure, Success, wrapResult } from 'lib-common/api'
import type { ApplicationAttachmentType } from 'lib-common/generated/api-types/application'
import type { IncomeStatementAttachmentType } from 'lib-common/generated/api-types/incomestatement'
import type {
  ApplicationId,
  AttachmentId,
  FeeAlterationId,
  IncomeId,
  IncomeStatementId,
  InvoiceId,
  MessageDraftId,
  PedagogicalDocumentId
} from 'lib-common/generated/api-types/shared'
import type { UploadHandler } from 'lib-components/molecules/FileUpload'

import {
  deleteAttachment,
  getAttachment,
  uploadApplicationAttachmentEmployee,
  uploadFeeAlterationAttachment,
  uploadIncomeAttachment,
  uploadIncomeStatementAttachmentEmployee,
  uploadInvoiceAttachmentEmployee,
  uploadMessageAttachment,
  uploadOrphanFeeAlterationAttachment,
  uploadOrphanIncomeAttachment,
  uploadPedagogicalDocumentAttachment
} from '../generated/api-clients/attachment'

function uploadHandler(
  upload: (
    file: File,
    onUploadProgress: (event: AxiosProgressEvent) => void
  ) => Promise<AttachmentId>
): UploadHandler {
  return {
    upload: async (file, onUploadProgress) => {
      try {
        const data = await upload(file, ({ loaded, total }) =>
          onUploadProgress(
            total !== undefined && total !== 0
              ? Math.round((loaded * 100) / total)
              : 0
          )
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
  applicationId: ApplicationId,
  type: ApplicationAttachmentType
): UploadHandler {
  return uploadHandler((file, onUploadProgress) =>
    uploadApplicationAttachmentEmployee(
      { file, applicationId, type },
      { onUploadProgress }
    )
  )
}

export function incomeStatementAttachment(
  incomeStatementId: IncomeStatementId,
  attachmentType: IncomeStatementAttachmentType
): UploadHandler {
  return uploadHandler((file, onUploadProgress) =>
    uploadIncomeStatementAttachmentEmployee(
      { file, incomeStatementId, attachmentType },
      { onUploadProgress }
    )
  )
}

export function incomeAttachment(incomeId: IncomeId | null): UploadHandler {
  return uploadHandler((file, onUploadProgress) =>
    incomeId
      ? uploadIncomeAttachment({ file, incomeId }, { onUploadProgress })
      : uploadOrphanIncomeAttachment({ file }, { onUploadProgress })
  )
}

export function invoiceAttachment(invoiceId: InvoiceId): UploadHandler {
  return uploadHandler((file, onUploadProgress) =>
    uploadInvoiceAttachmentEmployee({ file, invoiceId }, { onUploadProgress })
  )
}

export function feeAlterationAttachment(
  feeAlterationId: FeeAlterationId | null
): UploadHandler {
  return uploadHandler((file, onUploadProgress) =>
    feeAlterationId
      ? uploadFeeAlterationAttachment(
          { file, feeAlterationId },
          { onUploadProgress }
        )
      : uploadOrphanFeeAlterationAttachment({ file }, { onUploadProgress })
  )
}

export function messageAttachment(draftId: MessageDraftId): UploadHandler {
  return uploadHandler((file, onUploadProgress) =>
    uploadMessageAttachment({ file, draftId }, { onUploadProgress })
  )
}

export function pedagogicalDocumentAttachment(
  documentId: PedagogicalDocumentId
): UploadHandler {
  return uploadHandler((file, onUploadProgress) =>
    uploadPedagogicalDocumentAttachment(
      { file, documentId },
      { onUploadProgress }
    )
  )
}

export function getAttachmentUrl(
  attachmentId: AttachmentId,
  requestedFilename: string
): string {
  return getAttachment({ attachmentId, requestedFilename }).url.toString()
}
