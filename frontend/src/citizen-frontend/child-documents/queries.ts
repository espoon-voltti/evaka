// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0 } from 'lib-common/types'

import {
  getDocument,
  getDocuments,
  getUnreadDocumentsCount,
  putDocumentRead
} from '../generated/api-clients/document'
import { createQueryKeys } from '../query'

const queryKeys = createQueryKeys('childDocuments', {
  summaries: (args: Arg0<typeof getDocuments>) => ['summaries', args],
  details: (args: Arg0<typeof getDocument>) => ['details', args],
  unreadCount: () => ['unreadCount']
})

export const childDocumentSummariesQuery = query({
  api: getDocuments,
  queryKey: queryKeys.summaries
})

export const childDocumentDetailsQuery = query({
  api: getDocument,
  queryKey: queryKeys.details
})

export const unreadChildDocumentsCountQuery = query({
  api: getUnreadDocumentsCount,
  queryKey: queryKeys.unreadCount
})

export const childDocumentReadMutation = mutation({
  api: putDocumentRead,
  invalidateQueryKeys: () => [queryKeys.unreadCount()]
})
