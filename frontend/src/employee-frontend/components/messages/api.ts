// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  deserializeMessageCopy,
  deserializeMessageThread,
  deserializeReplyResponse
} from 'lib-common/api-types/messaging'
import {
  AuthorizedMessageAccount,
  DraftContent,
  MessageReceiversResponse,
  MessageThread,
  PagedMessageCopies,
  PagedMessageThreads,
  PagedSentMessages,
  PostMessageBody,
  ReplyToMessageBody,
  ThreadReply,
  UnreadCountByAccount
} from 'lib-common/generated/api-types/messaging'
import { JsonOf } from 'lib-common/json'
import {
  deserializeDraftContent,
  deserializeSentMessage
} from 'lib-common/messaging'
import { UUID } from 'lib-common/types'
import { SaveDraftParams } from 'lib-components/messages/types'

import { client } from '../../api/client'

export async function getReceivers(): Promise<
  Result<MessageReceiversResponse[]>
> {
  return client
    .get<JsonOf<MessageReceiversResponse[]>>('/messages/receivers')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getMessagingAccounts(): Promise<
  Result<AuthorizedMessageAccount[]>
> {
  return client
    .get<JsonOf<AuthorizedMessageAccount[]>>('/messages/my-accounts')
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export async function getUnreadCounts(): Promise<
  Result<UnreadCountByAccount[]>
> {
  return client
    .get<JsonOf<UnreadCountByAccount[]>>('/messages/unread')
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export async function getReceivedMessages(
  accountId: UUID,
  page: number,
  pageSize: number
): Promise<Result<PagedMessageThreads>> {
  return client
    .get<JsonOf<PagedMessageThreads>>(`/messages/${accountId}/received`, {
      params: { page, pageSize }
    })
    .then(({ data }) =>
      Success.of({
        ...data,
        data: data.data.map((d) => deserializeMessageThread(d))
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function getArchivedMessages(
  accountId: UUID,
  page: number,
  pageSize: number
): Promise<Result<PagedMessageThreads>> {
  return client
    .get<JsonOf<PagedMessageThreads>>(`/messages/${accountId}/archived`, {
      params: { page, pageSize }
    })
    .then(({ data }) =>
      Success.of({
        ...data,
        data: data.data.map((d) => deserializeMessageThread(d))
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function getMessageCopies(
  accountId: UUID,
  page: number,
  pageSize: number
): Promise<Result<PagedMessageCopies>> {
  return client
    .get<JsonOf<PagedMessageCopies>>(`/messages/${accountId}/copies`, {
      params: { page, pageSize }
    })
    .then(({ data }) =>
      Success.of({ ...data, data: data.data.map(deserializeMessageCopy) })
    )
    .catch((e) => Failure.fromError(e))
}

export async function getSentMessages(
  accountId: UUID,
  page: number,
  pageSize: number
): Promise<Result<PagedSentMessages>> {
  return client
    .get<JsonOf<PagedSentMessages>>(`/messages/${accountId}/sent`, {
      params: { page, pageSize }
    })
    .then(({ data }) =>
      Success.of({
        ...data,
        data: data.data.map(deserializeSentMessage)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function getMessageDrafts(
  accountId: UUID
): Promise<Result<DraftContent[]>> {
  return client
    .get<JsonOf<DraftContent[]>>(`/messages/${accountId}/drafts`)
    .then(({ data }) => Success.of(data.map(deserializeDraftContent)))
    .catch((e) => Failure.fromError(e))
}

export async function initDraft(accountId: UUID): Promise<Result<UUID>> {
  return client
    .post<UUID>(`/messages/${accountId}/drafts`)
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export async function saveDraft({
  accountId,
  draftId,
  content
}: SaveDraftParams): Promise<Result<void>> {
  return client
    .put(`/messages/${accountId}/drafts/${draftId}`, content)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function deleteDraft(
  accountId: UUID,
  draftId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/messages/${accountId}/drafts/${draftId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export type ReplyToThreadParams = ReplyToMessageBody & {
  messageId: UUID
  accountId: UUID
}
export async function replyToThread({
  messageId,
  content,
  accountId,
  recipientAccountIds
}: ReplyToThreadParams): Promise<Result<ThreadReply>> {
  return client
    .post<JsonOf<ThreadReply>>(`/messages/${accountId}/${messageId}/reply`, {
      content,
      recipientAccountIds
    })
    .then(({ data }) => Success.of(deserializeReplyResponse(data)))
    .catch((e) => Failure.fromError(e))
}

export async function postMessage(
  accountId: UUID,
  body: PostMessageBody
): Promise<Result<UUID | null>> {
  return client
    .post(`/messages/${accountId}`, body)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function markThreadRead(
  accountId: UUID,
  id: UUID
): Promise<Result<void>> {
  return client
    .put(`/messages/${accountId}/threads/${id}/read`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function archiveThread(
  accountId: UUID,
  id: UUID
): Promise<Result<void>> {
  return client
    .put(`/messages/${accountId}/threads/${id}/archive`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function undoMessage(
  accountId: UUID,
  contentId: UUID
): Promise<Result<UUID | null>> {
  return client
    .post(`/messages/${accountId}/undo-message`, null, {
      params: { contentId }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function undoMessageReply(
  accountId: UUID,
  messageId: UUID
): Promise<Result<UUID | null>> {
  return client
    .post(`/messages/${accountId}/undo-reply`, null, {
      params: { messageId }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getThread(
  accountId: UUID,
  threadId: UUID | null
): Promise<Result<MessageThread>> {
  if (threadId === null) {
    return Failure.of({ message: 'threadId is null' })
  }
  return client
    .get<JsonOf<MessageThread>>(`/messages/${accountId}/thread/${threadId}`)
    .then(({ data }) => Success.of(deserializeMessageThread(data)))
    .catch((e) => Failure.fromError(e))
}

export async function getMessageThreadForApplication(
  applicationId: UUID
): Promise<Result<MessageThread>> {
  return client
    .get<JsonOf<MessageThread>>(`/messages/application/${applicationId}`)
    .then(({ data }) => Success.of(deserializeMessageThread(data)))
    .catch((e) => Failure.fromError(e))
}
