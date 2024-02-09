// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import {
  Message,
  MessageRecipientType,
  MessageThread,
  ThreadReply
} from '../generated/api-types/messaging'
import HelsinkiDateTime from '../helsinki-date-time'
import { JsonOf } from '../json'

export type MessageReceiver =
  | MessageReceiverBase // unit in area, or child, or citizen
  | MessageReceiverArea
  | MessageReceiverUnit
  | MessageReceiverGroup

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
  receivers: MessageReceiverBase[]
}

export const deserializeMessage = (message: JsonOf<Message>): Message => ({
  ...message,
  sentAt: HelsinkiDateTime.parseIso(message.sentAt),
  readAt: message.readAt ? HelsinkiDateTime.parseIso(message.readAt) : null
})

export const deserializeMessageThread = (
  json: JsonOf<MessageThread>
): MessageThread => ({
  ...json,
  messages: json.messages.map(deserializeMessage)
})

export const deserializeReplyResponse = (
  responseData: JsonOf<ThreadReply>
): ThreadReply => ({
  threadId: responseData.threadId,
  message: deserializeMessage(responseData.message)
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
