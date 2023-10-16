// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { infiniteQuery, mutation, query } from 'lib-common/query'

import { createQueryKeys } from '../query'

import {
  getMessagingAccounts,
  getReceivedMessages,
  getUnreadCountsByUnit,
  markThreadRead,
  replyToThread
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
  unreadCounts: () => ['unreadCounts']
})

export const messagingAccountsQuery = query({
  // employeeId is not sent to api but is used to invalidate the query when it changes, so that there's no risk of
  // leaking account information from the previous logged-in employee
  api: ({ unitId }: { unitId: string; employeeId: string | undefined }) =>
    getMessagingAccounts(unitId),
  queryKey: queryKeys.accounts
})

export const receivedMessagesQuery = infiniteQuery({
  api: (accountId: string, pageSize: number) => (page: number) =>
    getReceivedMessages(accountId, page, pageSize),
  queryKey: queryKeys.receivedMessages,
  firstPageParam: 1,
  getNextPageParam: (lastPage, pages) => {
    const nextPage = pages.length + 1
    return nextPage <= lastPage.pages ? nextPage : undefined
  }
})

export const unreadCountsQuery = query({
  api: getUnreadCountsByUnit,
  queryKey: queryKeys.unreadCounts
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
