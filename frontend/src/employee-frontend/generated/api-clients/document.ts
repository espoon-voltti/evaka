// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import DateRange from 'lib-common/date-range'
import { ChildDocumentCreateRequest } from 'lib-common/generated/api-types/document'
import { ChildDocumentId } from 'lib-common/generated/api-types/shared'
import { ChildDocumentSummaryWithPermittedActions } from 'lib-common/generated/api-types/document'
import { ChildDocumentWithPermittedActions } from 'lib-common/generated/api-types/document'
import { DocumentContent } from 'lib-common/generated/api-types/document'
import { DocumentLockResponse } from 'lib-common/generated/api-types/document'
import { DocumentTemplate } from 'lib-common/generated/api-types/document'
import { DocumentTemplateBasicsRequest } from 'lib-common/generated/api-types/document'
import { DocumentTemplateContent } from 'lib-common/generated/api-types/document'
import { DocumentTemplateId } from 'lib-common/generated/api-types/shared'
import { DocumentTemplateSummary } from 'lib-common/generated/api-types/document'
import { ExportedDocumentTemplate } from 'lib-common/generated/api-types/document'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { StatusChangeRequest } from 'lib-common/generated/api-types/document'
import { Uri } from 'lib-common/uri'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonChildDocumentSummaryWithPermittedActions } from 'lib-common/generated/api-types/document'
import { deserializeJsonChildDocumentWithPermittedActions } from 'lib-common/generated/api-types/document'
import { deserializeJsonDocumentLockResponse } from 'lib-common/generated/api-types/document'
import { deserializeJsonDocumentTemplate } from 'lib-common/generated/api-types/document'
import { deserializeJsonDocumentTemplateSummary } from 'lib-common/generated/api-types/document'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.createTemplate
*/
export async function createTemplate(
  request: {
    body: DocumentTemplateBasicsRequest
  }
): Promise<DocumentTemplate> {
  const { data: json } = await client.request<JsonOf<DocumentTemplate>>({
    url: uri`/employee/document-templates`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DocumentTemplateBasicsRequest>
  })
  return deserializeJsonDocumentTemplate(json)
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.deleteDraftTemplate
*/
export async function deleteDraftTemplate(
  request: {
    templateId: DocumentTemplateId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/document-templates/${request.templateId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.duplicateTemplate
*/
export async function duplicateTemplate(
  request: {
    templateId: DocumentTemplateId,
    body: DocumentTemplateBasicsRequest
  }
): Promise<DocumentTemplate> {
  const { data: json } = await client.request<JsonOf<DocumentTemplate>>({
    url: uri`/employee/document-templates/${request.templateId}/duplicate`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DocumentTemplateBasicsRequest>
  })
  return deserializeJsonDocumentTemplate(json)
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.exportTemplate
*/
export function exportTemplate(
  request: {
    templateId: DocumentTemplateId
  }
): { url: Uri } {
  return {
    url: uri`/employee/document-templates/${request.templateId}/export`.withBaseUrl(client.defaults.baseURL ?? '')
  }
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.forceUnpublishTemplate
*/
export async function forceUnpublishTemplate(
  request: {
    templateId: DocumentTemplateId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/document-templates/${request.templateId}/force-unpublish`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.getActiveTemplates
*/
export async function getActiveTemplates(
  request: {
    childId: PersonId
  }
): Promise<DocumentTemplateSummary[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<DocumentTemplateSummary[]>>({
    url: uri`/employee/document-templates/active`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonDocumentTemplateSummary(e))
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.getTemplate
*/
export async function getTemplate(
  request: {
    templateId: DocumentTemplateId
  }
): Promise<DocumentTemplate> {
  const { data: json } = await client.request<JsonOf<DocumentTemplate>>({
    url: uri`/employee/document-templates/${request.templateId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonDocumentTemplate(json)
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.getTemplates
*/
export async function getTemplates(): Promise<DocumentTemplateSummary[]> {
  const { data: json } = await client.request<JsonOf<DocumentTemplateSummary[]>>({
    url: uri`/employee/document-templates`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonDocumentTemplateSummary(e))
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.importTemplate
*/
export async function importTemplate(
  request: {
    body: ExportedDocumentTemplate
  }
): Promise<DocumentTemplate> {
  const { data: json } = await client.request<JsonOf<DocumentTemplate>>({
    url: uri`/employee/document-templates/import`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ExportedDocumentTemplate>
  })
  return deserializeJsonDocumentTemplate(json)
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.publishTemplate
*/
export async function publishTemplate(
  request: {
    templateId: DocumentTemplateId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/document-templates/${request.templateId}/publish`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.updateDraftTemplateBasics
*/
export async function updateDraftTemplateBasics(
  request: {
    templateId: DocumentTemplateId,
    body: DocumentTemplateBasicsRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/document-templates/${request.templateId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<DocumentTemplateBasicsRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.updateDraftTemplateContent
*/
export async function updateDraftTemplateContent(
  request: {
    templateId: DocumentTemplateId,
    body: DocumentTemplateContent
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/document-templates/${request.templateId}/content`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<DocumentTemplateContent>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.updateTemplateValidity
*/
export async function updateTemplateValidity(
  request: {
    templateId: DocumentTemplateId,
    body: DateRange
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/document-templates/${request.templateId}/validity`.toString(),
    method: 'PUT',
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
  }
): Promise<ChildDocumentId> {
  const { data: json } = await client.request<JsonOf<ChildDocumentId>>({
    url: uri`/employee/child-documents`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ChildDocumentCreateRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.deleteDraftDocument
*/
export async function deleteDraftDocument(
  request: {
    documentId: ChildDocumentId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/child-documents/${request.documentId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.downloadChildDocument
*/
export function downloadChildDocument(
  request: {
    documentId: ChildDocumentId
  }
): { url: Uri } {
  return {
    url: uri`/employee/child-documents/${request.documentId}/pdf`.withBaseUrl(client.defaults.baseURL ?? '')
  }
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.getDocument
*/
export async function getDocument(
  request: {
    documentId: ChildDocumentId
  }
): Promise<ChildDocumentWithPermittedActions> {
  const { data: json } = await client.request<JsonOf<ChildDocumentWithPermittedActions>>({
    url: uri`/employee/child-documents/${request.documentId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonChildDocumentWithPermittedActions(json)
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.getDocuments
*/
export async function getDocuments(
  request: {
    childId: PersonId
  }
): Promise<ChildDocumentSummaryWithPermittedActions[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<ChildDocumentSummaryWithPermittedActions[]>>({
    url: uri`/employee/child-documents`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonChildDocumentSummaryWithPermittedActions(e))
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.nextDocumentStatus
*/
export async function nextDocumentStatus(
  request: {
    documentId: ChildDocumentId,
    body: StatusChangeRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/child-documents/${request.documentId}/next-status`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<StatusChangeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.planArchiveChildDocument
*/
export async function planArchiveChildDocument(
  request: {
    documentId: ChildDocumentId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/child-documents/${request.documentId}/archive`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.prevDocumentStatus
*/
export async function prevDocumentStatus(
  request: {
    documentId: ChildDocumentId,
    body: StatusChangeRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/child-documents/${request.documentId}/prev-status`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<StatusChangeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.publishDocument
*/
export async function publishDocument(
  request: {
    documentId: ChildDocumentId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/child-documents/${request.documentId}/publish`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.takeDocumentWriteLock
*/
export async function takeDocumentWriteLock(
  request: {
    documentId: ChildDocumentId
  }
): Promise<DocumentLockResponse> {
  const { data: json } = await client.request<JsonOf<DocumentLockResponse>>({
    url: uri`/employee/child-documents/${request.documentId}/lock`.toString(),
    method: 'PUT'
  })
  return deserializeJsonDocumentLockResponse(json)
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.updateDocumentContent
*/
export async function updateDocumentContent(
  request: {
    documentId: ChildDocumentId,
    body: DocumentContent
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/child-documents/${request.documentId}/content`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<DocumentContent>
  })
  return json
}
