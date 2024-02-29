// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import {
  getDocument,
  getDocuments,
  getUnreadDocumentsCount,
  putDocumentRead
} from '../generated/api-clients/document'
import { createQueryKeys } from '../query'

const queryKeys = createQueryKeys('childDocuments', {
  summaries: () => ['summaries'],
  details: (id: UUID) => ['details', id],
  unreadCount: () => ['unreadCount']
})

export const childDocumentSummariesQuery = query({
  api: getDocuments,
  queryKey: queryKeys.summaries
})

export const childDocumentDetailsQuery = query({
  api: getDocument,
  queryKey: ({ documentId }) => queryKeys.details(documentId)
})

export const unreadChildDocumentsCountQuery = query({
  api: getUnreadDocumentsCount,
  queryKey: queryKeys.unreadCount
})

export const childDocumentReadMutation = mutation({
  api: putDocumentRead,
  invalidateQueryKeys: () => [queryKeys.unreadCount()]
})
