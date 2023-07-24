// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../../query'

import {
  deleteVasuDocument,
  getVasuDocumentSummaries,
  updateDocumentState,
  UpdateDocumentStateParams
} from './api'

const queryKeys = createQueryKeys('vasu', {
  documentSummaries: (childId: UUID) => ['documentSummaries', childId]
})

export const vasuDocumentSummariesQuery = query({
  api: getVasuDocumentSummaries,
  queryKey: queryKeys.documentSummaries
})

export const updateDocumentStateMutation = mutation({
  api: ({
    documentId,
    eventType
  }: UpdateDocumentStateParams & { childId: UUID }) =>
    updateDocumentState({
      documentId,
      eventType
    }),
  invalidateQueryKeys: ({ childId }) => [queryKeys.documentSummaries(childId)]
})

export const deleteVasuDocumentMutation = mutation({
  api: ({ documentId }: { childId: UUID; documentId: UUID }) =>
    deleteVasuDocument(documentId),
  invalidateQueryKeys: ({ childId }) => [queryKeys.documentSummaries(childId)]
})
