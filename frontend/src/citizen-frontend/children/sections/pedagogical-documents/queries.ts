// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../../../query'

import {
  getPedagogicalDocuments,
  getUnreadPedagogicalDocumentsCount,
  markPedagogicalDocumentAsRead
} from './api'

const queryKeys = createQueryKeys('pedagogicalDocuments', {
  forChild: (childId: UUID) => ['documents', childId],
  unreadCount: () => ['unreadCount']
})

export const unreadPedagogicalDocumentsCountQuery = query({
  api: getUnreadPedagogicalDocumentsCount,
  queryKey: queryKeys.unreadCount
})

export const pedagogicalDocumentsQuery = query({
  api: getPedagogicalDocuments,
  queryKey: queryKeys.forChild
})

export const markPedagogicalDocumentAsReadMutation = mutation({
  api: (arg: { documentId: UUID; childId: UUID }) =>
    markPedagogicalDocumentAsRead(arg.documentId),
  invalidateQueryKeys: ({ childId }) => [
    pedagogicalDocumentsQuery(childId).queryKey,
    unreadPedagogicalDocumentsCountQuery().queryKey
  ]
})
