// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'

import {
  getDocumentTemplateSummaries,
  postDocumentTemplate
} from '../../api/document-templates'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('documentTemplates', {
  documentTemplateSummaries: () => ['documentTemplateSummaries']
})

export const documentTemplateSummariesQuery = query({
  api: getDocumentTemplateSummaries,
  queryKey: queryKeys.documentTemplateSummaries
})

export const createDocumentTemplateMutation = mutation({
  api: postDocumentTemplate,
  invalidateQueryKeys: () => [queryKeys.documentTemplateSummaries()]
})
