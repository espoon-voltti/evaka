// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import {
  getPedagogicalDocumentsForChild,
  getUnreadPedagogicalDocumentCount,
  markPedagogicalDocumentRead
} from '../../../generated/api-clients/pedagogicaldocument'
import { createQueryKeys } from '../../../query'

const queryKeys = createQueryKeys('pedagogicalDocuments', {
  forChild: (childId: UUID) => ['documents', childId],
  unreadCount: () => ['unreadCount']
})

export const unreadPedagogicalDocumentsCountQuery = query({
  api: getUnreadPedagogicalDocumentCount,
  queryKey: queryKeys.unreadCount
})

export const pedagogicalDocumentsQuery = query({
  api: getPedagogicalDocumentsForChild,
  queryKey: ({ childId }) => queryKeys.forChild(childId)
})

export const markPedagogicalDocumentAsReadMutation = mutation({
  api: (arg: Arg0<typeof markPedagogicalDocumentRead> & { childId: UUID }) =>
    markPedagogicalDocumentRead(arg),
  invalidateQueryKeys: ({ childId }) => [
    pedagogicalDocumentsQuery({ childId }).queryKey,
    unreadPedagogicalDocumentsCountQuery().queryKey
  ]
})
