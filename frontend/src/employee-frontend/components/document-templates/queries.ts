// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import {
  DocumentTemplateContent,
  DocumentTemplateCreateRequest,
  ExportedDocumentTemplate
} from 'lib-common/generated/api-types/document'
import { JsonOf } from 'lib-common/json'
import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import {
  deleteDocumentTemplate,
  getActiveDocumentTemplateSummaries,
  getDocumentTemplate,
  getDocumentTemplateSummaries,
  importDocumentTemplate,
  postDocumentTemplate,
  postDocumentTemplateDuplicate,
  putDocumentTemplateContent,
  putDocumentTemplatePublish,
  putDocumentTemplateValidity
} from '../../api/document-templates'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('documentTemplates', {
  documentTemplateSummaries: () => ['documentTemplateSummaries'],
  documentTemplate: (templateId: UUID) => ['documentTemplates', templateId],
  activeDocumentTemplateSummaries: (childId: UUID) => [
    'activeDocumentTemplateSummaries',
    childId
  ]
})

export const documentTemplateSummariesQuery = query({
  api: getDocumentTemplateSummaries,
  queryKey: queryKeys.documentTemplateSummaries
})

export const activeDocumentTemplateSummariesQuery = query({
  api: getActiveDocumentTemplateSummaries,
  queryKey: queryKeys.activeDocumentTemplateSummaries
})

export const documentTemplateQuery = query({
  api: getDocumentTemplate,
  queryKey: queryKeys.documentTemplate
})

export const createDocumentTemplateMutation = mutation({
  api: postDocumentTemplate,
  invalidateQueryKeys: () => [queryKeys.documentTemplateSummaries()]
})

export const duplicateDocumentTemplateMutation = mutation({
  api: (arg: { id: UUID; data: DocumentTemplateCreateRequest }) =>
    postDocumentTemplateDuplicate(arg.id, arg.data),
  invalidateQueryKeys: () => [queryKeys.documentTemplateSummaries()]
})

export const importDocumentTemplateMutation = mutation({
  api: (data: JsonOf<ExportedDocumentTemplate>) => importDocumentTemplate(data),
  invalidateQueryKeys: () => [queryKeys.documentTemplateSummaries()]
})

export const updateDocumentTemplateContentMutation = mutation({
  api: (arg: { id: UUID; content: DocumentTemplateContent }) =>
    putDocumentTemplateContent(arg.id, arg.content),
  invalidateQueryKeys: (arg) => [
    queryKeys.documentTemplateSummaries(),
    queryKeys.documentTemplate(arg.id)
  ]
})

export const updateDocumentTemplateValidityMutation = mutation({
  api: (arg: { id: UUID; validity: DateRange }) =>
    putDocumentTemplateValidity(arg.id, arg.validity),
  invalidateQueryKeys: (arg) => [
    queryKeys.documentTemplateSummaries(),
    queryKeys.documentTemplate(arg.id)
  ]
})

export const publishDocumentTemplateMutation = mutation({
  api: (id: string) => putDocumentTemplatePublish(id),
  invalidateQueryKeys: (id) => [
    queryKeys.documentTemplateSummaries(),
    queryKeys.documentTemplate(id)
  ]
})

export const deleteDocumentTemplateMutation = mutation({
  api: (id: string) => deleteDocumentTemplate(id),
  invalidateQueryKeys: () => [queryKeys.documentTemplateSummaries()]
})
