// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  createTemplate,
  deleteDraftTemplate,
  duplicateTemplate,
  forceUnpublishTemplate,
  getActiveTemplates,
  getTemplate,
  getTemplates,
  importTemplate,
  publishTemplate,
  updateDraftTemplateBasics,
  updateDraftTemplateContent,
  updateTemplateValidity
} from '../../generated/api-clients/document'

const q = new Queries()

export const documentTemplateSummariesQuery = q.query(getTemplates)

export const activeDocumentTemplateSummariesQuery = q.query(getActiveTemplates)

export const documentTemplateQuery = q.query(getTemplate)

export const createDocumentTemplateMutation = q.mutation(createTemplate, [
  documentTemplateSummariesQuery
])

export const duplicateDocumentTemplateMutation = q.mutation(duplicateTemplate, [
  documentTemplateSummariesQuery
])

export const importDocumentTemplateMutation = q.mutation(importTemplate, [
  documentTemplateSummariesQuery
])

export const updateDocumentTemplateBasicsMutation = q.mutation(
  updateDraftTemplateBasics,
  [
    documentTemplateSummariesQuery,
    ({ templateId }) => documentTemplateQuery({ templateId })
  ]
)

export const updateDocumentTemplateContentMutation = q.mutation(
  updateDraftTemplateContent,
  [
    documentTemplateSummariesQuery,
    ({ templateId }) => documentTemplateQuery({ templateId })
  ]
)

export const updateDocumentTemplateValidityMutation = q.mutation(
  updateTemplateValidity,
  [
    documentTemplateSummariesQuery,
    ({ templateId }) => documentTemplateQuery({ templateId })
  ]
)

export const publishDocumentTemplateMutation = q.mutation(publishTemplate, [
  documentTemplateSummariesQuery,
  ({ templateId }) => documentTemplateQuery({ templateId })
])

export const forceUnpublishDocumentTemplateMutation = q.mutation(
  forceUnpublishTemplate,
  [
    documentTemplateSummariesQuery,
    ({ templateId }) => documentTemplateQuery({ templateId })
  ]
)

export const deleteDocumentTemplateMutation = q.mutation(deleteDraftTemplate, [
  documentTemplateSummariesQuery
])
