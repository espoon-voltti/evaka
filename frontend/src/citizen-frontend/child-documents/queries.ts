// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../query'

import {
  getChildDocumentDetails,
  getChildDocumentSummaries,
  getUnreadChildDocumentsCount,
  markChildDocumentRead
} from './api'

const queryKeys = createQueryKeys('childDocuments', {
  summaries: () => ['summaries'],
  details: (id: UUID) => ['details', id],
  unreadCount: () => ['unreadCount']
})

export const childDocumentSummariesQuery = query({
  api: getChildDocumentSummaries,
  queryKey: queryKeys.summaries
})

export const childDocumentDetailsQuery = query({
  api: getChildDocumentDetails,
  queryKey: queryKeys.details
})

export const unreadChildDocumentsCountQuery = query({
  api: getUnreadChildDocumentsCount,
  queryKey: queryKeys.unreadCount
})

export const childDocumentReadMutation = mutation({
  api: markChildDocumentRead,
  invalidateQueryKeys: () => [queryKeys.unreadCount()]
})
