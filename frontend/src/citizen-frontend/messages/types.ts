// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

export interface MessageAccount {
  id: UUID
  name: string
}

export interface Message {
  id: UUID
  senderId: UUID
  senderName: string
  recipients: MessageAccount[]
  sentAt: Date
  readAt: Date | null
  content: string
}
export const deserializeMessage = (json: JsonOf<Message>): Message => ({
  ...json,
  sentAt: new Date(json.sentAt),
  readAt: json.readAt ? new Date(json.readAt) : null
})

export type MessageType = 'MESSAGE' | 'BULLETIN'

export interface MessageThread {
  id: UUID
  type: MessageType
  title: string
  messages: Message[]
}
export const deserializeMessageThread = (
  json: JsonOf<MessageThread>
): MessageThread => ({
  ...json,
  messages: json.messages.map(deserializeMessage)
})

export interface ReplyResponse {
  threadId: UUID
  message: Message
}
