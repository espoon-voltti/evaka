// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { infiniteQuery, mutation, query } from 'lib-common/query'

import { createQueryKeys } from '../query'

import { getMessagingAccounts, getReceivedMessages, replyToThread } from './api'

const queryKeys = createQueryKeys('messages', {
  accounts: (unitId: string) => ['accounts', unitId],
  receivedMessages: (accountId: string) => ['receivedMessages', accountId]
})

export const messagingAccountsQuery = query({
  api: getMessagingAccounts,
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

export const replyToThreadMutation = mutation({
  api: replyToThread,
  invalidateQueryKeys: ({ accountId }) => [
    queryKeys.receivedMessages(accountId)
  ]
})
