// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import DateRange from 'lib-common/date-range'
import { ChildDocumentCreateRequest } from 'lib-common/generated/api-types/document'
import { ChildDocumentSummaryWithPermittedActions } from 'lib-common/generated/api-types/document'
import { ChildDocumentWithPermittedActions } from 'lib-common/generated/api-types/document'
import { DocumentContent } from 'lib-common/generated/api-types/document'
import { DocumentTemplate } from 'lib-common/generated/api-types/document'
import { DocumentTemplateContent } from 'lib-common/generated/api-types/document'
import { DocumentTemplateCreateRequest } from 'lib-common/generated/api-types/document'
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
import { deserializeJsonDocumentTemplate } from 'lib-common/generated/api-types/document'
import { deserializeJsonDocumentTemplateSummary } from 'lib-common/generated/api-types/document'
import { deserializeJsonExportedDocumentTemplate } from 'lib-common/generated/api-types/document'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.createTemplate
*/
export async function createTemplate(
  request: {
    body: DocumentTemplateCreateRequest
  }
): Promise<DocumentTemplate> {
  const { data: json } = await client.request<JsonOf<DocumentTemplate>>({
    url: uri`/document-templates`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DocumentTemplateCreateRequest>
  })
  return deserializeJsonDocumentTemplate(json)
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.deleteDraftTemplate
*/
export async function deleteDraftTemplate(
  request: {
    templateId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/document-templates/${request.templateId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.duplicateTemplate
*/
export async function duplicateTemplate(
  request: {
    templateId: UUID,
    body: DocumentTemplateCreateRequest
  }
): Promise<DocumentTemplate> {
  const { data: json } = await client.request<JsonOf<DocumentTemplate>>({
    url: uri`/document-templates/${request.templateId}/duplicate`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DocumentTemplateCreateRequest>
  })
  return deserializeJsonDocumentTemplate(json)
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.exportTemplate
*/
export async function exportTemplate(
  request: {
    templateId: UUID
  }
): Promise<ExportedDocumentTemplate> {
  const { data: json } = await client.request<JsonOf<ExportedDocumentTemplate>>({
    url: uri`/document-templates/${request.templateId}/export`.toString(),
    method: 'GET'
  })
  return deserializeJsonExportedDocumentTemplate(json)
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.getActiveTemplates
*/
export async function getActiveTemplates(
  request: {
    childId: UUID
  }
): Promise<DocumentTemplateSummary[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<DocumentTemplateSummary[]>>({
    url: uri`/document-templates/active`.toString(),
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
    templateId: UUID
  }
): Promise<DocumentTemplate> {
  const { data: json } = await client.request<JsonOf<DocumentTemplate>>({
    url: uri`/document-templates/${request.templateId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonDocumentTemplate(json)
}


/**
* Generated from fi.espoo.evaka.document.DocumentTemplateController.getTemplates
*/
export async function getTemplates(): Promise<DocumentTemplateSummary[]> {
  const { data: json } = await client.request<JsonOf<DocumentTemplateSummary[]>>({
    url: uri`/document-templates`.toString(),
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
    url: uri`/document-templates/import`.toString(),
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
    templateId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/document-templates/${request.templateId}/publish`.toString(),
    method: 'PUT'
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
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/document-templates/${request.templateId}/content`.toString(),
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
    templateId: UUID,
    body: DateRange
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/document-templates/${request.templateId}/validity`.toString(),
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
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/child-documents`.toString(),
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
    documentId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/child-documents/${request.documentId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.getDocument
*/
export async function getDocument(
  request: {
    documentId: UUID
  }
): Promise<ChildDocumentWithPermittedActions> {
  const { data: json } = await client.request<JsonOf<ChildDocumentWithPermittedActions>>({
    url: uri`/child-documents/${request.documentId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonChildDocumentWithPermittedActions(json)
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.getDocuments
*/
export async function getDocuments(
  request: {
    childId: UUID
  }
): Promise<ChildDocumentSummaryWithPermittedActions[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<ChildDocumentSummaryWithPermittedActions[]>>({
    url: uri`/child-documents`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonChildDocumentSummaryWithPermittedActions(e))
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.nextStatus
*/
export async function nextStatus(
  request: {
    documentId: UUID,
    body: StatusChangeRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/child-documents/${request.documentId}/next-status`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<StatusChangeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.prevStatus
*/
export async function prevStatus(
  request: {
    documentId: UUID,
    body: StatusChangeRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/child-documents/${request.documentId}/prev-status`.toString(),
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
    documentId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/child-documents/${request.documentId}/publish`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.document.childdocument.ChildDocumentController.updateDocumentContent
*/
export async function updateDocumentContent(
  request: {
    documentId: UUID,
    body: DocumentContent
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/child-documents/${request.documentId}/content`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<DocumentContent>
  })
  return json
}
