// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Paged, Result, Success } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from './client'
import {
  MessageThread,
  NestedMessageAccount,
  ReplyToMessageBody,
  ThreadReply,
  UnreadCountByAccount
} from 'lib-common/generated/api-types/messaging'
import {
  deserializeMessageThread,
  deserializeReplyResponse
} from 'lib-common/api-types/messaging/message'

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

// todo unitId not needed after we implement proper authentication mechanism
export async function getUnreadCounts(
  unitId: UUID
): Promise<Result<UnreadCountByAccount[]>> {
  return client
    .get<JsonOf<UnreadCountByAccount[]>>(`/messages/mobile/unread/${unitId}`)
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export async function getReceivedMessages(
  accountId: UUID,
  page: number,
  pageSize: number
): Promise<Result<Paged<MessageThread>>> {
  return client
    .get<JsonOf<Paged<MessageThread>>>(
      `/messages/mobile/${accountId}/received`,
      {
        params: { page, pageSize }
      }
    )
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
    .post<JsonOf<ThreadReply>>(
      `/messages/mobile/${accountId}/${messageId}/reply`,
      {
        content,
        recipientAccountIds
      }
    )
    .then(({ data }) => Success.of(deserializeReplyResponse(data)))
    .catch((e) => Failure.fromError(e))
}

export async function markThreadRead(
  accountId: UUID,
  id: UUID
): Promise<Result<void>> {
  return client
    .put(`/messages/mobile/${accountId}/threads/${id}/read`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}
