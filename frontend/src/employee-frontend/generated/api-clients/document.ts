// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import DateRange from 'lib-common/date-range'
import { AxiosHeaders } from 'axios'
import { ChildDocumentCreateRequest } from 'lib-common/generated/api-types/document'
import { ChildDocumentSummaryWithPermittedActions } from 'lib-common/generated/api-types/document'
import { ChildDocumentWithPermittedActions } from 'lib-common/generated/api-types/document'
import { DocumentContent } from 'lib-common/generated/api-types/document'
import { DocumentLockResponse } from 'lib-common/generated/api-types/document'
import { DocumentTemplate } from 'lib-common/generated/api-types/document'
import { DocumentTemplateBasicsRequest } from 'lib-common/generated/api-types/document'
import { DocumentTemplateContent } from 'lib-common/generated/api-types/document'
import { DocumentTemplateSummary } from 'lib-common/generated/api-types/document'
import { ExportedDocumentTemplate } from 'lib-common/generated/api-types/document'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { StatusChangeRequest } from 'lib-common/generated/api-types/document'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonChildDocumentSummaryWithPermittedActions } from 'lib-common/generated/api-types/document'
import { deserializeJsonChildDocumentWithPermittedActions } from 'lib-common/generated/api-types/document'
import { deserializeJsonDocumentLockResponse } from 'lib-common/generated/api-types/document'
import { deserializeJsonDocumentTemplate } from 'lib-common/generated/api-types/document'
import { deserializeJsonDocumentTemplateSummary } from 'lib-common/generated/api-types/document'
import { deserializeJsonExportedDocumentTemplate } from 'lib-common/generated/api-types/document'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.createTemplate
*/
export async function createTemplate(
  request: {
    body: DocumentTemplateBasicsRequest
  },
  headers?: AxiosHeaders
): Promise<DocumentTemplate> {
  const { data: json } = await client.request<JsonOf<DocumentTemplate>>({
    url: uri`/employee/document-templates`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<DocumentTemplateBasicsRequest>
  })
  return deserializeJsonDocumentTemplate(json)
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.deleteDraftTemplate
*/
export async function deleteDraftTemplate(
  request: {
    templateId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/document-templates/${request.templateId}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.duplicateTemplate
*/
export async function duplicateTemplate(
  request: {
    templateId: UUID,
    body: DocumentTemplateBasicsRequest
  },
  headers?: AxiosHeaders
): Promise<DocumentTemplate> {
  const { data: json } = await client.request<JsonOf<DocumentTemplate>>({
    url: uri`/employee/document-templates/${request.templateId}/duplicate`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<DocumentTemplateBasicsRequest>
  })
  return deserializeJsonDocumentTemplate(json)
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.exportTemplate
*/
export async function exportTemplate(
  request: {
    templateId: UUID
  },
  headers?: AxiosHeaders
): Promise<ExportedDocumentTemplate> {
  const { data: json } = await client.request<JsonOf<ExportedDocumentTemplate>>({
    url: uri`/employee/document-templates/${request.templateId}/export`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonExportedDocumentTemplate(json)
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.forceUnpublishTemplate
*/
export async function forceUnpublishTemplate(
  request: {
    templateId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/document-templates/${request.templateId}/force-unpublish`.toString(),
    method: 'PUT',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.getActiveTemplates
*/
export async function getActiveTemplates(
  request: {
    childId: UUID
  },
  headers?: AxiosHeaders
): Promise<DocumentTemplateSummary[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<DocumentTemplateSummary[]>>({
    url: uri`/employee/document-templates/active`.toString(),
    method: 'GET',
    headers,
    params
  })
  return json.map(e => deserializeJsonDocumentTemplateSummary(e))
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.getTemplate
*/
export async function getTemplate(
  request: {
    templateId: UUID
  },
  headers?: AxiosHeaders
): Promise<DocumentTemplate> {
  const { data: json } = await client.request<JsonOf<DocumentTemplate>>({
    url: uri`/employee/document-templates/${request.templateId}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonDocumentTemplate(json)
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.getTemplates
*/
export async function getTemplates(
  headers?: AxiosHeaders
): Promise<DocumentTemplateSummary[]> {
  const { data: json } = await client.request<JsonOf<DocumentTemplateSummary[]>>({
    url: uri`/employee/document-templates`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonDocumentTemplateSummary(e))
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.importTemplate
*/
export async function importTemplate(
  request: {
    body: ExportedDocumentTemplate
  },
  headers?: AxiosHeaders
): Promise<DocumentTemplate> {
  const { data: json } = await client.request<JsonOf<DocumentTemplate>>({
    url: uri`/employee/document-templates/import`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<ExportedDocumentTemplate>
  })
  return deserializeJsonDocumentTemplate(json)
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.publishTemplate
*/
export async function publishTemplate(
  request: {
    templateId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/document-templates/${request.templateId}/publish`.toString(),
    method: 'PUT',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.updateDraftTemplateBasics
*/
export async function updateDraftTemplateBasics(
  request: {
    templateId: UUID,
    body: DocumentTemplateBasicsRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/document-templates/${request.templateId}`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<DocumentTemplateBasicsRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.updateDraftTemplateContent
*/
export async function updateDraftTemplateContent(
  request: {
    templateId: UUID,
    body: DocumentTemplateContent
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/document-templates/${request.templateId}/content`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<DocumentTemplateContent>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.updateTemplateValidity
*/
export async function updateTemplateValidity(
  request: {
    templateId: UUID,
    body: DateRange
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/document-templates/${request.templateId}/validity`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<DateRange>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.createDocument
*/
export async function createDocument(
  request: {
    body: ChildDocumentCreateRequest
  },
  headers?: AxiosHeaders
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee/child-documents`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<ChildDocumentCreateRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.deleteDraftDocument
*/
export async function deleteDraftDocument(
  request: {
    documentId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/child-documents/${request.documentId}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.getDocument
*/
export async function getDocument(
  request: {
    documentId: UUID
  },
  headers?: AxiosHeaders
): Promise<ChildDocumentWithPermittedActions> {
  const { data: json } = await client.request<JsonOf<ChildDocumentWithPermittedActions>>({
    url: uri`/employee/child-documents/${request.documentId}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonChildDocumentWithPermittedActions(json)
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.getDocuments
*/
export async function getDocuments(
  request: {
    childId: UUID
  },
  headers?: AxiosHeaders
): Promise<ChildDocumentSummaryWithPermittedActions[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<ChildDocumentSummaryWithPermittedActions[]>>({
    url: uri`/employee/child-documents`.toString(),
    method: 'GET',
    headers,
    params
  })
  return json.map(e => deserializeJsonChildDocumentSummaryWithPermittedActions(e))
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.nextDocumentStatus
*/
export async function nextDocumentStatus(
  request: {
    documentId: UUID,
    body: StatusChangeRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/child-documents/${request.documentId}/next-status`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<StatusChangeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.prevDocumentStatus
*/
export async function prevDocumentStatus(
  request: {
    documentId: UUID,
    body: StatusChangeRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/child-documents/${request.documentId}/prev-status`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<StatusChangeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.publishDocument
*/
export async function publishDocument(
  request: {
    documentId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/child-documents/${request.documentId}/publish`.toString(),
    method: 'PUT',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.takeDocumentWriteLock
*/
export async function takeDocumentWriteLock(
  request: {
    documentId: UUID
  },
  headers?: AxiosHeaders
): Promise<DocumentLockResponse> {
  const { data: json } = await client.request<JsonOf<DocumentLockResponse>>({
    url: uri`/employee/child-documents/${request.documentId}/lock`.toString(),
    method: 'PUT',
    headers
  })
  return deserializeJsonDocumentLockResponse(json)
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.updateDocumentContent
*/
export async function updateDocumentContent(
  request: {
    documentId: UUID,
    body: DocumentContent
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/child-documents/${request.documentId}/content`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<DocumentContent>
  })
  return json
}
