// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Paged, Result, Success } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from './client'
import {
  MessageReceiversResponse,
  MessageThread,
  NestedMessageAccount,
  PostMessageBody,
  ReplyToMessageBody,
  ThreadReply,
  UnreadCountByAccountAndGroup
} from 'lib-common/generated/api-types/messaging'
import {
  deserializeMessageThread,
  deserializeReplyResponse
} from 'lib-common/api-types/messaging/message'
import {
  deserializeReceiver,
  SaveDraftParams
} from 'lib-components/employee/messages/types'

export async function getMessagingAccounts(
  unitId: UUID
): Promise<Result<NestedMessageAccount[]>> {
  return client
    .get<JsonOf<NestedMessageAccount[]>>(
      `/messages/mobile/my-accounts/${unitId}`
    )
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export async function getUnreadCountsByUnit(
  unitId: UUID
): Promise<Result<UnreadCountByAccountAndGroup[]>> {
  return client
    .get<JsonOf<UnreadCountByAccountAndGroup[]>>(`/messages/unread/${unitId}`)
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
        data: data.data.map((d) => deserializeMessageThread(d))
      })
    )
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

export async function markThreadRead(
  accountId: UUID,
  id: UUID
): Promise<Result<void>> {
  return client
    .put(`/messages/${accountId}/threads/${id}/read`)
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

export async function postMessage(
  accountId: UUID,
  body: PostMessageBody
): Promise<Result<void>> {
  return client
    .post(`/messages/${accountId}`, body)
    .then(() => Success.of(undefined))
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

export async function getReceivers(
  unitId: UUID
): Promise<Result<MessageReceiversResponse[]>> {
  return client
    .get<JsonOf<MessageReceiversResponse[]>>('/messages/receivers', {
      params: { unitId }
    })
    .then((res) =>
      Success.of(
        res.data.map((receiverGroup) => ({
          ...receiverGroup,
          receivers: receiverGroup.receivers.map(deserializeReceiver)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}
