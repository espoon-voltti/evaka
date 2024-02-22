// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import {
  createTemplate,
  deleteDraftTemplate,
  duplicateTemplate,
  getActiveTemplates,
  getTemplate,
  getTemplates,
  importTemplate,
  publishTemplate,
  updateDraftTemplateContent,
  updateTemplateValidity
} from '../../generated/api-clients/document'
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
  api: getTemplates,
  queryKey: queryKeys.documentTemplateSummaries
})

export const activeDocumentTemplateSummariesQuery = query({
  api: getActiveTemplates,
  queryKey: ({ childId }) => queryKeys.activeDocumentTemplateSummaries(childId)
})

export const documentTemplateQuery = query({
  api: getTemplate,
  queryKey: ({ templateId }) => queryKeys.documentTemplate(templateId)
})

export const createDocumentTemplateMutation = mutation({
  api: createTemplate,
  invalidateQueryKeys: () => [queryKeys.documentTemplateSummaries()]
})

export const duplicateDocumentTemplateMutation = mutation({
  api: duplicateTemplate,
  invalidateQueryKeys: () => [queryKeys.documentTemplateSummaries()]
})

export const importDocumentTemplateMutation = mutation({
  api: importTemplate,
  invalidateQueryKeys: () => [queryKeys.documentTemplateSummaries()]
})

export const updateDocumentTemplateContentMutation = mutation({
  api: updateDraftTemplateContent,
  invalidateQueryKeys: (arg) => [
    queryKeys.documentTemplateSummaries(),
    queryKeys.documentTemplate(arg.templateId)
  ]
})

export const updateDocumentTemplateValidityMutation = mutation({
  api: updateTemplateValidity,
  invalidateQueryKeys: (arg) => [
    queryKeys.documentTemplateSummaries(),
    queryKeys.documentTemplate(arg.templateId)
  ]
})

export const publishDocumentTemplateMutation = mutation({
  api: publishTemplate,
  invalidateQueryKeys: (arg) => [
    queryKeys.documentTemplateSummaries(),
    queryKeys.documentTemplate(arg.templateId)
  ]
})

export const deleteDocumentTemplateMutation = mutation({
  api: deleteDraftTemplate,
  invalidateQueryKeys: () => [queryKeys.documentTemplateSummaries()]
})
