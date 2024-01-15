// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import {
  DocumentTemplate,
  DocumentTemplateContent,
  DocumentTemplateCreateRequest,
  DocumentTemplateSummary,
  ExportedDocumentTemplate
} from 'lib-common/generated/api-types/document'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

import { API_URL, client } from './client'

export async function postDocumentTemplate(
  data: DocumentTemplateCreateRequest
): Promise<DocumentTemplate> {
  return client
    .post<JsonOf<DocumentTemplate>>('/document-templates', data)
    .then((res) => ({
      ...res.data,
      validity: DateRange.parseJson(res.data.validity)
    }))
}

export async function importDocumentTemplate(
  data: JsonOf<ExportedDocumentTemplate>
): Promise<DocumentTemplate> {
  return client
    .post<JsonOf<DocumentTemplate>>('/document-templates/import', data)
    .then((res) => ({
      ...res.data,
      validity: DateRange.parseJson(res.data.validity)
    }))
}

export async function postDocumentTemplateDuplicate(
  id: UUID,
  data: DocumentTemplateCreateRequest
): Promise<DocumentTemplate> {
  return client
    .post<JsonOf<DocumentTemplate>>(`/document-templates/${id}/duplicate`, data)
    .then((res) => ({
      ...res.data,
      validity: DateRange.parseJson(res.data.validity)
    }))
}

export async function getDocumentTemplateSummaries(): Promise<
  DocumentTemplateSummary[]
> {
  return client
    .get<JsonOf<DocumentTemplateSummary[]>>('/document-templates')
    .then((res) =>
      res.data.map((template) => ({
        ...template,
        validity: DateRange.parseJson(template.validity)
      }))
    )
}

export async function getActiveDocumentTemplateSummaries(
  childId: UUID
): Promise<DocumentTemplateSummary[]> {
  return client
    .get<JsonOf<DocumentTemplateSummary[]>>('/document-templates/active', {
      params: { childId }
    })
    .then((res) =>
      res.data.map((template) => ({
        ...template,
        validity: DateRange.parseJson(template.validity)
      }))
    )
}

export async function getDocumentTemplate(id: UUID): Promise<DocumentTemplate> {
  return client
    .get<JsonOf<DocumentTemplate>>(`/document-templates/${id}`)
    .then((res) => ({
      ...res.data,
      validity: DateRange.parseJson(res.data.validity)
    }))
}

export async function putDocumentTemplateContent(
  id: UUID,
  content: DocumentTemplateContent
): Promise<void> {
  await client.put(`/document-templates/${id}/content`, content)
}

export async function putDocumentTemplateValidity(
  id: UUID,
  validity: DateRange
): Promise<void> {
  await client.put(`/document-templates/${id}/validity`, validity)
}

export async function putDocumentTemplatePublish(id: UUID): Promise<void> {
  await client.put(`/document-templates/${id}/publish`)
}

export async function deleteDocumentTemplate(id: UUID): Promise<void> {
  await client.delete(`/document-templates/${id}`)
}

export function exportDocumentTemplateUrl(id: UUID): string {
  return `${API_URL}/document-templates/${encodeURIComponent(id)}/export`
}
