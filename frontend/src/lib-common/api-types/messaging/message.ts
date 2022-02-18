// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Message,
  MessageAccount,
  MessageThread,
  ThreadReply
} from '../../generated/api-types/messaging'
import { JsonOf } from '../../json'

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
