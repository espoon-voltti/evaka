// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ApplicationAttachmentType } from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import type { AttachmentId } from 'lib-common/generated/api-types/shared'
import type { IncomeStatementAttachmentType } from 'lib-common/generated/api-types/incomestatement'
import type { IncomeStatementId } from 'lib-common/generated/api-types/shared'
import type { JsonOf } from 'lib-common/json'
import type { Uri } from 'lib-common/uri'
import { AxiosProgressEvent } from 'axios'
import { client } from '../../api-client'
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
    url: uri`/citizen/attachments/${request.attachmentId}`.toString(),
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
    url: uri`/citizen/attachments/${request.attachmentId}/download/${request.requestedFilename}`.withBaseUrl(client.defaults.baseURL ?? '')
  }
}


/**
* Generated from fi.espoo.evaka.attachment.AttachmentsController.uploadApplicationAttachmentCitizen
*/
export async function uploadApplicationAttachmentCitizen(
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
    url: uri`/citizen/attachments/applications/${request.applicationId}`.toString(),
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
* Generated from fi.espoo.evaka.attachment.AttachmentsController.uploadIncomeStatementAttachmentCitizen
*/
export async function uploadIncomeStatementAttachmentCitizen(
  request: {
    incomeStatementId: IncomeStatementId,
    attachmentType?: IncomeStatementAttachmentType | null,
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
    ['attachmentType', request.attachmentType?.toString()]
  )
  const { data: json } = await client.request<JsonOf<AttachmentId>>({
    url: uri`/citizen/attachments/income-statements/${request.incomeStatementId}`.toString(),
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
* Generated from fi.espoo.evaka.attachment.AttachmentsController.uploadMessageAttachmentCitizen
*/
export async function uploadMessageAttachmentCitizen(
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
    url: uri`/citizen/attachments/messages`.toString(),
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
* Generated from fi.espoo.evaka.attachment.AttachmentsController.uploadOrphanIncomeStatementAttachmentCitizen
*/
export async function uploadOrphanIncomeStatementAttachmentCitizen(
  request: {
    attachmentType?: IncomeStatementAttachmentType | null,
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
    ['attachmentType', request.attachmentType?.toString()]
  )
  const { data: json } = await client.request<JsonOf<AttachmentId>>({
    url: uri`/citizen/attachments/income-statements`.toString(),
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
