// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, pagedInfiniteQuery, query } from 'lib-common/query'

import {
  archiveThread,
  getMyAccount,
  getReceivedMessages,
  getReceivers,
  getUnreadMessages,
  markThreadRead,
  newMessage,
  replyToThread
} from '../generated/api-clients/messaging'
import { createQueryKeys } from '../query'

const queryKeys = createQueryKeys('messages', {
  receivedMessages: () => ['receivedMessages'],
  receivers: () => ['receivers'],
  unreadMessagesCount: () => ['unreadMessagesCount'],
  messageAccount: () => ['messageAccount']
})

export const receivedMessagesQuery = pagedInfiniteQuery({
  api: () => (page: number) => getReceivedMessages({ page }),
  queryKey: queryKeys.receivedMessages,
  id: (thread) => thread.id
})

export const receiversQuery = query({
  api: getReceivers,
  queryKey: queryKeys.receivers
})

export const messageAccountQuery = query({
  api: getMyAccount,
  queryKey: queryKeys.messageAccount
})

export const unreadMessagesCountQuery = query({
  api: getUnreadMessages,
  queryKey: queryKeys.unreadMessagesCount
})

export const markThreadReadMutation = mutation({
  api: markThreadRead,
  invalidateQueryKeys: () => [queryKeys.unreadMessagesCount()]
})

export const sendMessageMutation = mutation({
  api: newMessage,
  invalidateQueryKeys: () => [queryKeys.receivedMessages()]
})

export const replyToThreadMutation = mutation({
  api: replyToThread,
  invalidateQueryKeys: () => [queryKeys.receivedMessages()]
})

export const archiveThreadMutation = mutation({
  api: archiveThread,
  invalidateQueryKeys: () => [queryKeys.receivedMessages()]
})
