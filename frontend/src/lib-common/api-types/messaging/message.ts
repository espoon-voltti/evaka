// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from '../../json'
import { UUID } from '../../types'

export type MessageType = 'MESSAGE' | 'BULLETIN'

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
export const deserializeMessage = (m: JsonOf<Message>): Message => ({
  ...m,
  sentAt: new Date(m.sentAt),
  readAt: m.readAt ? new Date(m.readAt) : null
})

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
export const deserializeReplyResponse = ({
  message,
  threadId
}: JsonOf<ReplyResponse>) => ({
  threadId,
  message: deserializeMessage(message)
})
