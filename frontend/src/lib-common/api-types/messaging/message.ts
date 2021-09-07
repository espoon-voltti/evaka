// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from '../../json'
import { UUID } from '../../types'
import { MessageType } from 'lib-common/generated/enums'

type AccountType = 'PERSONAL' | 'GROUP' | 'CITIZEN'

export interface MessageAccount {
  id: UUID
  name: string
  type: AccountType
}

export interface Message {
  id: UUID
  sender: MessageAccount
  recipients: MessageAccount[]
  sentAt: Date
  readAt: Date | null
  content: string
}

export const deserializeMessageAccount = (
  account: JsonOf<MessageAccount>,
  staffAnnotation?: string
): MessageAccount => ({
  ...account,
  name:
    account.type === 'GROUP' && staffAnnotation
      ? `${account.name} (${staffAnnotation})`
      : account.name
})

export const deserializeMessage = (
  message: JsonOf<Message>,
  staffAnnotation?: string
): Message => ({
  ...message,
  sender: deserializeMessageAccount(message.sender, staffAnnotation),
  recipients: message.recipients.map((a) =>
    deserializeMessageAccount(a, staffAnnotation)
  ),
  sentAt: new Date(message.sentAt),
  readAt: message.readAt ? new Date(message.readAt) : null
})

export interface MessageThread {
  id: UUID
  type: MessageType
  title: string
  messages: Message[]
}
export const deserializeMessageThread = (
  json: JsonOf<MessageThread>,
  staffAnnotation?: string
): MessageThread => ({
  ...json,
  messages: json.messages.map((m) => deserializeMessage(m, staffAnnotation))
})

export interface ReplyResponse {
  threadId: UUID
  message: Message
}
export const deserializeReplyResponse = (
  responseData: JsonOf<ReplyResponse>,
  staffAnnotation?: string
) => ({
  threadId: responseData.threadId,
  message: deserializeMessage(responseData.message, staffAnnotation)
})
