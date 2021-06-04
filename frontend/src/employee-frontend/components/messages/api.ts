// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Paged, Result, Success } from 'lib-common/api'
import {
  deserializeMessageThread,
  deserializeReplyResponse,
  MessageThread,
  ReplyResponse
} from 'lib-common/api-types/messaging/message'
import { JsonOf } from 'lib-common/json'
import { client } from '../../api/client'
import { UUID } from '../../types'
import {
  deserializeDraftContent,
  deserializeReceiverChild,
  deserializeSentMessage,
  DraftContent,
  MessageAccount,
  MessageBody,
  ReceiverGroup,
  SentMessage,
  UpsertableDraftContent
} from './types'

export async function getReceivers(
  unitId: UUID
): Promise<Result<ReceiverGroup[]>> {
  return client
    .get<JsonOf<ReceiverGroup[]>>('/messages/receivers', {
      params: { unitId }
    })
    .then((res) =>
      Success.of(
        res.data.map((receiverGroup) => ({
          ...receiverGroup,
          receivers: receiverGroup.receivers.map(deserializeReceiverChild)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getMessagingAccounts(): Promise<
  Result<MessageAccount[]>
> {
  return client
    .get<JsonOf<MessageAccount[]>>('/messages/my-accounts')
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export async function getReceivedMessages(
  accountId: UUID,
  page: number,
  pageSize: number
): Promise<Result<Paged<MessageThread>>> {
  return client
    .get<JsonOf<Paged<MessageThread>>>(`/messages/${accountId}/received`, {
      params: { page, pageSize }
    })
    .then(({ data }) =>
      Success.of({
        ...data,
        data: data.data.map(deserializeMessageThread)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function getSentMessages(
  accountId: UUID,
  page: number,
  pageSize: number
): Promise<Result<Paged<SentMessage>>> {
  return client
    .get<JsonOf<Paged<SentMessage>>>(`/messages/${accountId}/sent`, {
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

export interface SaveDraftParams {
  accountId: UUID
  draftId: UUID
  content: UpsertableDraftContent
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

export interface ReplyToThreadParams {
  messageId: UUID
  content: string
  accountId: UUID
  recipientAccountIds: UUID[]
}
export async function replyToThread({
  messageId,
  content,
  accountId,
  recipientAccountIds
}: ReplyToThreadParams): Promise<Result<ReplyResponse>> {
  return client
    .post<JsonOf<ReplyResponse>>(`/messages/${accountId}/${messageId}/reply`, {
      content,
      recipientAccountIds
    })
    .then(({ data }) => Success.of(deserializeReplyResponse(data)))
    .catch((e) => Failure.fromError(e))
}

export async function postMessage(
  accountId: UUID,
  body: MessageBody
): Promise<Result<void>> {
  return client
    .post(`/messages/${accountId}`, body)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function markThreadRead(accountId: UUID, id: UUID): Promise<void> {
  return client.put(`/messages/${accountId}/threads/${id}/read`)
}
