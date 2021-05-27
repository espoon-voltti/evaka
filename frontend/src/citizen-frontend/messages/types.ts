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
  messages: json.messages.map((m) => ({
    ...m,
    sentAt: new Date(m.sentAt),
    readAt: m.readAt ? new Date(m.readAt) : null
  }))
})
