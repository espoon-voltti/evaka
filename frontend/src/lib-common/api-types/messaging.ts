// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import {
  Message,
  MessageAccount,
  MessageCopy,
  MessageRecipientType,
  MessageThread,
  ThreadReply
} from '../generated/api-types/messaging'
import HelsinkiDateTime from '../helsinki-date-time'
import { JsonOf } from '../json'

export type MessageReceiver =
  | MessageReceiverArea
  | MessageReceiverUnitInArea
  | MessageReceiverUnit
  | MessageReceiverGroup
  | MessageReceiverChild

interface MessageReceiverBase {
  id: UUID
  name: string
  type: MessageRecipientType
}

interface MessageReceiverArea extends MessageReceiverBase {
  receivers: MessageReceiverUnitInArea[]
}

type MessageReceiverUnitInArea = MessageReceiverBase

interface MessageReceiverUnit extends MessageReceiverBase {
  receivers: MessageReceiverGroup[]
}

interface MessageReceiverGroup extends MessageReceiverBase {
  receivers: MessageReceiverChild[]
}

type MessageReceiverChild = MessageReceiverBase

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
  sentAt: HelsinkiDateTime.parseIso(message.sentAt),
  readAt: message.readAt ? HelsinkiDateTime.parseIso(message.readAt) : null
})

export const deserializeMessageThread = (
  json: JsonOf<MessageThread>,
  staffAnnotation?: string
): MessageThread => ({
  ...json,
  messages: json.messages.map((m) => deserializeMessage(m, staffAnnotation))
})

export const deserializeReplyResponse = (
  responseData: JsonOf<ThreadReply>,
  staffAnnotation?: string
): ThreadReply => ({
  threadId: responseData.threadId,
  message: deserializeMessage(responseData.message, staffAnnotation)
})

export const deserializeMessageCopy = (
  json: JsonOf<MessageCopy>
): MessageCopy => ({
  ...json,
  sentAt: HelsinkiDateTime.parseIso(json.sentAt),
  readAt: json.readAt ? HelsinkiDateTime.parseIso(json.readAt) : null
})

export const sortReceivers = (
  receivers: MessageReceiver[]
): MessageReceiver[] =>
  receivers
    .map((receiver) =>
      'receivers' in receiver
        ? {
            ...receiver,
            receivers:
              receiver.receivers.length === 0
                ? []
                : sortReceivers(receiver.receivers)
          }
        : receiver
    )
    .sort((a, b) =>
      a.name.toLocaleLowerCase().localeCompare(b.name.toLocaleLowerCase())
    )
