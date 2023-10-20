// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, pagedInfiniteQuery, query } from 'lib-common/query'

import { createQueryKeys } from '../query'

import {
  archiveThread,
  getMessageAccount,
  getReceivedMessages,
  getReceivers,
  getUnreadMessagesCount,
  markThreadRead,
  replyToThread,
  sendMessage
} from './api'

const queryKeys = createQueryKeys('messages', {
  receivedMessages: () => ['receivedMessages'],
  receivers: () => ['receivers'],
  unreadMessagesCount: () => ['unreadMessagesCount'],
  messageAccount: () => ['messageAccount']
})

export const receivedMessagesQuery = pagedInfiniteQuery({
  api: (pageSize: number) => (page: number) =>
    getReceivedMessages(page, pageSize),
  queryKey: queryKeys.receivedMessages,
  id: (thread) => thread.id
})

export const receiversQuery = query({
  api: getReceivers,
  queryKey: queryKeys.receivers
})

export const messageAccountQuery = query({
  api: getMessageAccount,
  queryKey: queryKeys.messageAccount
})

export const unreadMessagesCountQuery = query({
  api: getUnreadMessagesCount,
  queryKey: queryKeys.unreadMessagesCount
})

export const markThreadReadMutation = mutation({
  api: markThreadRead,
  invalidateQueryKeys: () => [queryKeys.unreadMessagesCount()]
})

export const sendMessageMutation = mutation({
  api: sendMessage,
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
