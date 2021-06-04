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
import { UUID } from 'lib-common/types'
import { client } from '../api-client'

export async function getReceivedMessages(
  page: number,
  pageSize = 10
): Promise<Result<Paged<MessageThread>>> {
  return client
    .get<JsonOf<Paged<MessageThread>>>('/citizen/messages/received', {
      params: { page, pageSize }
    })
    .then((res) =>
      Success.of({
        ...res.data,
        data: res.data.data.map(deserializeMessageThread)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function getMessageAccount(): Promise<Result<UUID>> {
  return client
    .get<UUID>(`/citizen/messages/my-account`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function markThreadRead(id: string): Promise<void> {
  return client.put(`/citizen/messages/threads/${id}/read`)
}

export async function getUnreadMessagesCount(): Promise<Result<number>> {
  return client
    .get<number>(`/citizen/messages/unread-count`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export interface ReplyToThreadParams {
  messageId: UUID
  content: string
  recipientAccountIds: UUID[]
}

export async function replyToThread({
  messageId,
  content,
  recipientAccountIds
}: ReplyToThreadParams): Promise<Result<ReplyResponse>> {
  return client
    .post<JsonOf<ReplyResponse>>(`/citizen/messages/${messageId}/reply`, {
      content,
      recipientAccountIds
    })
    .then(({ data }) => Success.of(deserializeReplyResponse(data)))
    .catch((e) => Failure.fromError(e))
}
