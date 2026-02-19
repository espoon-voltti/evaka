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
  getArchivedMessages,
  getDraftMessages,
  getFinanceMessagesWithPerson,
  getFolders,
  getMessageCopies,
  getMessagesInFolder,
  getReceivedMessages,
  getSelectableRecipients,
  getSentMessages,
  getThread,
  getUnreadMessages,
  initDraftMessage,
  markLastReceivedMessageInThreadUnread,
  markThreadRead,
  moveThreadToFolder,
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

export const draftsQuery = q.query(getDraftMessages)

export const financeThreadsQuery = q.query(getFinanceMessagesWithPerson)

export const receivedMessagesQuery = q.query(getReceivedMessages)

export const sentMessagesQuery = q.query(getSentMessages)

export const messageCopiesQuery = q.query(getMessageCopies)

export const archivedMessagesQuery = q.query(getArchivedMessages)

export const messagesInFolderQuery = q.query(getMessagesInFolder)

export const threadQuery = q.query(getThread)

export const selectableRecipientsQuery = q.query(getSelectableRecipients)

export const markThreadReadMutation = q.mutation(markThreadRead, [
  unreadMessagesQuery,
  receivedMessagesQuery.prefix
])

export const markLastReceivedMessageInThreadUnreadMutation = q.mutation(
  markLastReceivedMessageInThreadUnread,
  [unreadMessagesQuery]
)

export const initDraftMutation = q.mutation(initDraftMessage, [draftsQuery])

export const saveDraftMutation = q.mutation(updateDraftMessage, [draftsQuery])

export const deleteDraftMutation = q.mutation(deleteDraftMessage, [draftsQuery])

export const replyToThreadMutation = q.mutation(replyToThread, [
  receivedMessagesQuery.prefix,
  messagesInFolderQuery.prefix,
  threadQuery.prefix
])

export const archiveThreadMutation = q.mutation(archiveThread, [
  receivedMessagesQuery.prefix,
  archivedMessagesQuery.prefix,
  messagesInFolderQuery.prefix,
  threadQuery.prefix
])

export const moveThreadToFolderMutation = q.mutation(moveThreadToFolder, [
  receivedMessagesQuery.prefix,
  messagesInFolderQuery.prefix
])

export const createMessageMutation = q.mutation(createMessage, [
  sentMessagesQuery.prefix,
  receivedMessagesQuery.prefix,
  draftsQuery.prefix,
  messageCopiesQuery.prefix,
  archivedMessagesQuery.prefix,
  messagesInFolderQuery.prefix,
  threadQuery.prefix
])

export const createFinanceThreadMutation = q.parametricMutation<{
  id: PersonId
}>()(createMessage, [
  ({ id }) => financeThreadsQuery({ personId: id }),
  sentMessagesQuery.prefix,
  draftsQuery.prefix
])

export const replyToFinanceThreadMutation = q.parametricMutation<{
  id: PersonId
}>()(replyToThread, [
  ({ id }) => financeThreadsQuery({ personId: id }),
  receivedMessagesQuery.prefix,
  messagesInFolderQuery.prefix,
  threadQuery.prefix
])

export const archiveFinanceThreadMutation = q.parametricMutation<{
  id: PersonId
}>()(archiveThread, [
  ({ id }) => financeThreadsQuery({ personId: id }),
  receivedMessagesQuery.prefix,
  archivedMessagesQuery.prefix,
  messagesInFolderQuery.prefix,
  threadQuery.prefix
])
