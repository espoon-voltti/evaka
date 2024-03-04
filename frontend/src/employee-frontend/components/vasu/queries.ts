// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import {
  deleteDocument,
  getVasuSummariesByChild,
  updateDocumentState
} from '../../generated/api-clients/vasu'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('vasu', {
  documentSummaries: (childId: UUID) => ['documentSummaries', childId]
})

export const vasuDocumentSummariesQuery = query({
  api: getVasuSummariesByChild,
  queryKey: ({ childId }) => queryKeys.documentSummaries(childId)
})

export const updateDocumentStateMutation = mutation({
  api: (arg: Arg0<typeof updateDocumentState> & { childId: UUID }) =>
    updateDocumentState(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.documentSummaries(childId)]
})

export const deleteVasuDocumentMutation = mutation({
  api: (arg: Arg0<typeof deleteDocument> & { childId: UUID }) =>
    deleteDocument(arg),
  invalidateQueryKeys: ({ childId }) => [queryKeys.documentSummaries(childId)]
})
