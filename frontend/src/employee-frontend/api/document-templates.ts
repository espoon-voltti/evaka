// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import {
  DocumentTemplate,
  DocumentTemplateCreateRequest,
  DocumentTemplateSummary
} from 'lib-common/generated/api-types/document'
import { JsonOf } from 'lib-common/json'

import { client } from './client'

export function postDocumentTemplate(
  data: DocumentTemplateCreateRequest
): Promise<DocumentTemplate> {
  return client
    .post<JsonOf<DocumentTemplate>>('/document-templates', data)
    .then((res) => ({
      ...res.data,
      validity: DateRange.parseJson(res.data.validity)
    }))
}

export function getDocumentTemplateSummaries(): Promise<
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
