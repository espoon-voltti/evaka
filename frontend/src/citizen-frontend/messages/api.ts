// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'

import { Paged } from 'lib-common/api'
import {
  deserializeMessage,
  deserializeReplyResponse
} from 'lib-common/api-types/messaging'
import {
  CitizenMessageBody,
  CitizenMessageThread,
  GetReceiversResponse,
  ReplyToMessageBody,
  ThreadReply
} from 'lib-common/generated/api-types/messaging'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

import { client } from '../api-client'

export const deserializeCitizenMessageThread = (
  json: JsonOf<CitizenMessageThread>
): CitizenMessageThread =>
  json.type === 'MESSAGE_THREAD'
    ? {
        ...json,
        messages: json.messages.map(deserializeMessage)
      }
    : {
        ...json,
        lastMessageSentAt: json.lastMessageSentAt
          ? HelsinkiDateTime.parseIso(json.lastMessageSentAt)
          : null
      }

export async function getReceivedMessages(
  page: number,
  pageSize: number
): Promise<Paged<CitizenMessageThread>> {
  return client
    .get<JsonOf<Paged<CitizenMessageThread>>>('/citizen/messages/received', {
      params: { page, pageSize }
    })
    .then((res) => ({
      ...res.data,
      data: res.data.data.map((d) => deserializeCitizenMessageThread(d))
    }))
}

export async function getReceivers(): Promise<GetReceiversResponse> {
  return client
    .get<GetReceiversResponse>(`/citizen/messages/receivers`)
    .then((res) => ({
      ...res.data,
      messageAccounts: sortBy(res.data.messageAccounts, 'name')
    }))
}

export async function getMessageAccount(): Promise<UUID> {
  return client
    .get<UUID>(`/citizen/messages/my-account`)
    .then((res) => res.data)
}

export async function markThreadRead(id: string): Promise<void> {
  return client
    .put(`/citizen/messages/threads/${id}/read`)
    .then(() => undefined)
}

export async function getUnreadMessagesCount(): Promise<number> {
  return client
    .get<number>(`/citizen/messages/unread-count`)
    .then((res) => res.data)
}

export async function sendMessage(message: CitizenMessageBody): Promise<UUID> {
  return client
    .post<JsonOf<UUID>>(`/citizen/messages`, message)
    .then(({ data }) => data)
}

export type ReplyToThreadParams = ReplyToMessageBody & {
  messageId: UUID
}

export async function replyToThread({
  messageId,
  content,
  recipientAccountIds
}: ReplyToThreadParams): Promise<ThreadReply> {
  return client
    .post<JsonOf<ThreadReply>>(`/citizen/messages/${messageId}/reply`, {
      content,
      recipientAccountIds
    })
    .then(({ data }) => deserializeReplyResponse(data))
}

export async function archiveThread(id: UUID): Promise<void> {
  return client
    .put(`/citizen/messages/threads/${id}/archive`)
    .then(() => undefined)
}
