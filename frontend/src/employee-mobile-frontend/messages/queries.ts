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
  getReceivers,
  getSentMessages,
  getMessageDrafts,
  initDraft,
  saveDraft,
  deleteDraft
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
  sentMessages: (accountId: string) => ['sentMessages', accountId],
  draftMessages: (accountId: string) => ['draftMessages', accountId],
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

const PAGE_SIZE = 20

export const receivedMessagesQuery = pagedInfiniteQuery({
  api: (accountId: string) => (page: number) =>
    getReceivedMessages(accountId, page, PAGE_SIZE),
  id: (thread) => thread.id,
  queryKey: queryKeys.receivedMessages
})

export const sentMessagesQuery = pagedInfiniteQuery({
  api: (accountId: string) => (page: number) =>
    getSentMessages(accountId, page, PAGE_SIZE),
  id: (message) => message.contentId,
  queryKey: queryKeys.sentMessages
})

export const draftMessagesQuery = query({
  api: getMessageDrafts,
  queryKey: queryKeys.draftMessages
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
  api: sendMessage,
  invalidateQueryKeys: ({ accountId }) => [
    queryKeys.sentMessages(accountId),
    queryKeys.draftMessages(accountId)
  ]
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

export const initDraftMutation = mutation({
  api: initDraft,
  invalidateQueryKeys: (accountId) => [queryKeys.draftMessages(accountId)]
})

export const saveDraftMutation = mutation({
  api: saveDraft,
  invalidateQueryKeys: ({ accountId }) => [queryKeys.draftMessages(accountId)]
})

export const deleteDraftMutation = mutation({
  api: deleteDraft,
  invalidateQueryKeys: ({ accountId }) => [queryKeys.draftMessages(accountId)]
})
