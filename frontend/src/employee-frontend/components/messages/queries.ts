// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { PersonId } from 'lib-common/generated/api-types/shared'
import { Queries } from 'lib-common/query'

import {
  archiveThread,
  createMessage,
  createMessagePreflightCheck,
  deleteDraftMessage,
  getAccountsByUser,
  getDraftMessages,
  getFinanceMessagesWithPerson,
  getFolders,
  getUnreadMessages,
  initDraftMessage,
  markLastReceivedMessageInThreadUnread,
  markThreadRead,
  replyToThread,
  updateDraftMessage
} from '../../generated/api-clients/messaging'

const q = new Queries()

export const accountsByUserQuery = q.query(getAccountsByUser)

export const foldersQuery = q.query(getFolders)

export const unreadMessagesQuery = q.query(getUnreadMessages)

export const createMessagePreflightCheckQuery = q.query(
  createMessagePreflightCheck
)

export const markThreadReadMutation = q.mutation(markThreadRead, [
  unreadMessagesQuery
])

export const replyToThreadMutation = q.mutation(replyToThread)

export const draftsQuery = q.query(getDraftMessages)

export const initDraftMutation = q.mutation(initDraftMessage, [draftsQuery])

export const saveDraftMutation = q.mutation(updateDraftMessage, [draftsQuery])

export const deleteDraftMutation = q.mutation(deleteDraftMessage, [draftsQuery])

export const financeThreadsQuery = q.query(getFinanceMessagesWithPerson)

export const createFinanceThreadMutation = q.parametricMutation<{
  id: PersonId
}>()(createMessage, [({ id }) => financeThreadsQuery({ personId: id })])

export const replyToFinanceThreadMutation = q.parametricMutation<{
  id: PersonId
}>()(replyToThread, [({ id }) => financeThreadsQuery({ personId: id })])

export const archiveFinanceThreadMutation = q.parametricMutation<{
  id: PersonId
}>()(archiveThread, [({ id }) => financeThreadsQuery({ personId: id })])

export const markLastReceivedMessageInThreadUnreadMutation = q.mutation(
  markLastReceivedMessageInThreadUnread,
  [unreadMessagesQuery]
)
