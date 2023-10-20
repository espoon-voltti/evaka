// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, pagedInfiniteQuery, query } from 'lib-common/query'

import { createQueryKeys } from '../query'

import {
  getMessagingAccounts,
  getReceivedMessages,
  getUnreadCountsByUnit,
  markThreadRead,
  sendMessage,
  replyToThread,
  getReceivers
} from './api'

const queryKeys = createQueryKeys('messages', {
  accounts: ({
    unitId,
    employeeId
  }: {
    unitId: string
    employeeId: string | undefined
  }) => ['accounts', unitId, employeeId],
  receivedMessages: (accountId: string) => ['receivedMessages', accountId],
  recipients: () => ['recipients'],
  unreadCounts: () => ['unreadCounts']
})

export const messagingAccountsQuery = query({
  // employeeId is not sent to api but is used to invalidate the query when it changes, so that there's no risk of
  // leaking account information from the previous logged-in employee
  api: ({ unitId }: { unitId: string; employeeId: string | undefined }) =>
    getMessagingAccounts(unitId),
  queryKey: queryKeys.accounts
})

export const receivedMessagesQuery = pagedInfiniteQuery({
  api: (accountId: string, pageSize: number) => (page: number) =>
    getReceivedMessages(accountId, page, pageSize),
  id: (thread) => thread.id,
  queryKey: queryKeys.receivedMessages
})

// The results are dependent on the PIN-logged user
export const recipientsQuery = query({
  api: getReceivers,
  queryKey: queryKeys.recipients
})

export const unreadCountsQuery = query({
  api: getUnreadCountsByUnit,
  queryKey: queryKeys.unreadCounts
})

export const sendMessageMutation = mutation({
  api: sendMessage
})

export const replyToThreadMutation = mutation({
  api: replyToThread,
  invalidateQueryKeys: ({ accountId }) => [
    queryKeys.receivedMessages(accountId)
  ]
})

export const markThreadReadMutation = mutation({
  api: markThreadRead,
  invalidateQueryKeys: () => [queryKeys.unreadCounts()]
})
