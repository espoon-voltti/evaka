// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { ApplicationAttachmentType } from 'lib-common/generated/api-types/application'
import { ApplicationId } from 'lib-common/generated/api-types/shared'
import { AttachmentId } from 'lib-common/generated/api-types/shared'
import { AxiosProgressEvent } from 'axios'
import { FeeAlterationId } from 'lib-common/generated/api-types/shared'
import { IncomeId } from 'lib-common/generated/api-types/shared'
import { IncomeStatementAttachmentType } from 'lib-common/generated/api-types/incomestatement'
import { IncomeStatementId } from 'lib-common/generated/api-types/shared'
import { InvoiceId } from 'lib-common/generated/api-types/shared'
import { JsonOf } from 'lib-common/json'
import { MessageDraftId } from 'lib-common/generated/api-types/shared'
import { PedagogicalDocumentId } from 'lib-common/generated/api-types/shared'
import { Uri } from 'lib-common/uri'
import { client } from '../../api/client'
import { createFormData } from 'lib-common/api'
import { createUrlSearchParams } from 'lib-common/api'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.attachment.AttachmentsController.deleteAttachment
*/
export async function deleteAttachment(
  request: {
    attachmentId: AttachmentId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/attachments/${request.attachmentId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attachment.AttachmentsController.getAttachment
*/
export function getAttachment(
  request: {
    attachmentId: AttachmentId,
    requestedFilename: string
  }
): { url: Uri } {
  return {
    url: uri`${client.defaults.baseURL ?? ''}/employee/attachments/${request.attachmentId}/download/${request.requestedFilename}`
  }
}


/**
* Generated from fi.espoo.evaka.attachment.AttachmentsController.uploadApplicationAttachmentEmployee
*/
export async function uploadApplicationAttachmentEmployee(
  request: {
    applicationId: ApplicationId,
    type: ApplicationAttachmentType,
    file: File
  },
  options?: {
    onUploadProgress?: (event: AxiosProgressEvent) => void
  }
): Promise<AttachmentId> {
  const data = createFormData(
    ['file', request.file]
  )
  const params = createUrlSearchParams(
    ['type', request.type.toString()]
  )
  const { data: json } = await client.request<JsonOf<AttachmentId>>({
    url: uri`/employee/attachments/applications/${request.applicationId}`.toString(),
    method: 'POST',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: options?.onUploadProgress,
    params,
    data
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attachment.AttachmentsController.uploadFeeAlterationAttachment
*/
export async function uploadFeeAlterationAttachment(
  request: {
    feeAlterationId: FeeAlterationId,
    file: File
  },
  options?: {
    onUploadProgress?: (event: AxiosProgressEvent) => void
  }
): Promise<AttachmentId> {
  const data = createFormData(
    ['file', request.file]
  )
  const { data: json } = await client.request<JsonOf<AttachmentId>>({
    url: uri`/employee/attachments/fee-alteration/${request.feeAlterationId}`.toString(),
    method: 'POST',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: options?.onUploadProgress,
    data
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attachment.AttachmentsController.uploadIncomeAttachment
*/
export async function uploadIncomeAttachment(
  request: {
    incomeId: IncomeId,
    file: File
  },
  options?: {
    onUploadProgress?: (event: AxiosProgressEvent) => void
  }
): Promise<AttachmentId> {
  const data = createFormData(
    ['file', request.file]
  )
  const { data: json } = await client.request<JsonOf<AttachmentId>>({
    url: uri`/employee/attachments/income/${request.incomeId}`.toString(),
    method: 'POST',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: options?.onUploadProgress,
    data
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attachment.AttachmentsController.uploadIncomeStatementAttachmentEmployee
*/
export async function uploadIncomeStatementAttachmentEmployee(
  request: {
    incomeStatementId: IncomeStatementId,
    attachmentType: IncomeStatementAttachmentType,
    file: File
  },
  options?: {
    onUploadProgress?: (event: AxiosProgressEvent) => void
  }
): Promise<AttachmentId> {
  const data = createFormData(
    ['file', request.file]
  )
  const params = createUrlSearchParams(
    ['attachmentType', request.attachmentType.toString()]
  )
  const { data: json } = await client.request<JsonOf<AttachmentId>>({
    url: uri`/employee/attachments/income-statements/${request.incomeStatementId}`.toString(),
    method: 'POST',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: options?.onUploadProgress,
    params,
    data
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attachment.AttachmentsController.uploadInvoiceAttachmentEmployee
*/
export async function uploadInvoiceAttachmentEmployee(
  request: {
    invoiceId: InvoiceId,
    file: File
  },
  options?: {
    onUploadProgress?: (event: AxiosProgressEvent) => void
  }
): Promise<AttachmentId> {
  const data = createFormData(
    ['file', request.file]
  )
  const { data: json } = await client.request<JsonOf<AttachmentId>>({
    url: uri`/employee/attachments/invoices/${request.invoiceId}`.toString(),
    method: 'POST',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: options?.onUploadProgress,
    data
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attachment.AttachmentsController.uploadMessageAttachment
*/
export async function uploadMessageAttachment(
  request: {
    draftId: MessageDraftId,
    file: File
  },
  options?: {
    onUploadProgress?: (event: AxiosProgressEvent) => void
  }
): Promise<AttachmentId> {
  const data = createFormData(
    ['file', request.file]
  )
  const { data: json } = await client.request<JsonOf<AttachmentId>>({
    url: uri`/employee/attachments/messages/${request.draftId}`.toString(),
    method: 'POST',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: options?.onUploadProgress,
    data
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attachment.AttachmentsController.uploadOrphanFeeAlterationAttachment
*/
export async function uploadOrphanFeeAlterationAttachment(
  request: {
    file: File
  },
  options?: {
    onUploadProgress?: (event: AxiosProgressEvent) => void
  }
): Promise<AttachmentId> {
  const data = createFormData(
    ['file', request.file]
  )
  const { data: json } = await client.request<JsonOf<AttachmentId>>({
    url: uri`/employee/attachments/fee-alteration`.toString(),
    method: 'POST',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: options?.onUploadProgress,
    data
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attachment.AttachmentsController.uploadOrphanIncomeAttachment
*/
export async function uploadOrphanIncomeAttachment(
  request: {
    file: File
  },
  options?: {
    onUploadProgress?: (event: AxiosProgressEvent) => void
  }
): Promise<AttachmentId> {
  const data = createFormData(
    ['file', request.file]
  )
  const { data: json } = await client.request<JsonOf<AttachmentId>>({
    url: uri`/employee/attachments/income`.toString(),
    method: 'POST',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: options?.onUploadProgress,
    data
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attachment.AttachmentsController.uploadPedagogicalDocumentAttachment
*/
export async function uploadPedagogicalDocumentAttachment(
  request: {
    documentId: PedagogicalDocumentId,
    file: File
  },
  options?: {
    onUploadProgress?: (event: AxiosProgressEvent) => void
  }
): Promise<AttachmentId> {
  const data = createFormData(
    ['file', request.file]
  )
  const { data: json } = await client.request<JsonOf<AttachmentId>>({
    url: uri`/employee/attachments/pedagogical-documents/${request.documentId}`.toString(),
    method: 'POST',
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: options?.onUploadProgress,
    data
  })
  return json
}
