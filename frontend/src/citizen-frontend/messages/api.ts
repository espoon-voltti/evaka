// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Paged, Result, Success } from 'lib-common/api'
import {
  deserializeMessageAccount,
  deserializeMessageThread,
  deserializeReplyResponse
} from 'lib-common/api-types/messaging/message'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import {
  CitizenMessageBody,
  MessageAccount,
  MessageThread,
  ReplyToMessageBody,
  ThreadReply
} from 'lib-common/generated/api-types/messaging'
import { client } from '../api-client'

export async function getReceivedMessages(
  page: number,
  staffAnnotation: string,
  pageSize = 10
): Promise<Result<Paged<MessageThread>>> {
  return client
    .get<JsonOf<Paged<MessageThread>>>('/citizen/messages/received', {
      params: { page, pageSize }
    })
    .then((res) =>
      Success.of({
        ...res.data,
        data: res.data.data.map((d) =>
          deserializeMessageThread(d, staffAnnotation)
        )
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function getReceivers(
  staffAnnotation: string
): Promise<Result<MessageAccount[]>> {
  return client
    .get<MessageAccount[]>(`/citizen/messages/receivers`)
    .then((res) =>
      Success.of(
        res.data.map((d) => deserializeMessageAccount(d, staffAnnotation))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getMessageAccount(): Promise<Result<UUID>> {
  return client
    .get<UUID>(`/citizen/messages/my-account`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function markThreadRead(id: string): Promise<Result<void>> {
  return client
    .put(`/citizen/messages/threads/${id}/read`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export type UnreadCountByAccount = {
  accountId: UUID
  unreadCount: number
}

export async function getUnreadMessagesCount(): Promise<
  Result<UnreadCountByAccount[]>
> {
  return client
    .get<UnreadCountByAccount[]>(`/citizen/messages/unread-count`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function sendMessage(
  message: CitizenMessageBody
): Promise<Result<UUID[]>> {
  return client
    .post<JsonOf<UUID[]>>(`/citizen/messages`, message)
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export type ReplyToThreadParams = ReplyToMessageBody & {
  messageId: UUID
  staffAnnotation: string
}

export async function replyToThread({
  messageId,
  content,
  recipientAccountIds,
  staffAnnotation
}: ReplyToThreadParams): Promise<Result<ThreadReply>> {
  return client
    .post<JsonOf<ThreadReply>>(`/citizen/messages/${messageId}/reply`, {
      content,
      recipientAccountIds
    })
    .then(({ data }) =>
      Success.of(deserializeReplyResponse(data, staffAnnotation))
    )
    .catch((e) => Failure.fromError(e))
}
