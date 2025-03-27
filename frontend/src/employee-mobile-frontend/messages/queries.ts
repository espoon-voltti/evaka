// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ChildId,
  EmployeeId,
  MessageAccountId,
  MessageThreadId
} from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import {
  createMessage,
  createMessagePreflightCheck,
  deleteDraftMessage,
  getAccountsByDevice,
  getDraftMessages,
  getReceivedMessages,
  getReceiversForNewMessage,
  getSentMessages,
  getThread,
  getUnreadMessagesByUnit,
  initDraftMessage,
  markThreadRead,
  replyToThread,
  updateDraftMessage
} from '../generated/api-clients/messaging'

const q = new Queries()

export const messagingAccountsQuery = q.parametricQuery<
  EmployeeId | undefined
>()(
  // employeeId is not sent to api but is used to invalidate the query when it changes, so that there's no risk of
  // leaking account information from the previous logged-in employee
  getAccountsByDevice
)

export const receivedMessagesQuery = q.pagedInfiniteQuery(
  (accountId: MessageAccountId, childId: ChildId | null = null) =>
    (page: number) =>
      getReceivedMessages({ accountId, page, childId }),
  (thread) => thread.id
)

export const sentMessagesQuery = q.pagedInfiniteQuery(
  (accountId: MessageAccountId) => (page: number) =>
    getSentMessages({ accountId, page }),
  (message) => message.contentId
)

export const draftMessagesQuery = q.query(getDraftMessages)

export const threadQuery = q.query(getThread)

// The results are dependent on the PIN-logged user
export const recipientsQuery = q.query(getReceiversForNewMessage)

export const unreadCountsQuery = q.query(getUnreadMessagesByUnit)

export const createMessagePreflightCheckQuery = q.query(
  createMessagePreflightCheck
)

export const sendMessageMutation = q.mutation(createMessage, [
  ({ accountId }) => sentMessagesQuery(accountId),
  ({ accountId }) => draftMessagesQuery({ accountId })
])

export const replyToThreadMutation = q.parametricMutation<{
  threadId: MessageThreadId
}>()(replyToThread, [
  ({ accountId }) => receivedMessagesQuery(accountId),
  ({ accountId, threadId }) => threadQuery({ accountId, threadId })
])

export const markThreadReadMutation = q.mutation(markThreadRead, [
  unreadCountsQuery.prefix
])

export const initDraftMutation = q.mutation(initDraftMessage, [
  ({ accountId }) => draftMessagesQuery({ accountId })
])

export const saveDraftMutation = q.mutation(updateDraftMessage, [
  ({ accountId }) => draftMessagesQuery({ accountId })
])

export const deleteDraftMutation = q.mutation(deleteDraftMessage, [
  ({ accountId }) => draftMessagesQuery({ accountId })
])
