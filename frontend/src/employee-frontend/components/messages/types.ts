// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from '../../types'

export type Recipient = {
  personId: string
  firstName: string
  lastName: string
  guardian: boolean
  headOfChild: boolean
  blocklisted: boolean
}

export type IdAndName = {
  id: UUID
  name: string
}

export interface ReceiverChild {
  childId: UUID
  childFirstName: string
  childLastName: string
  childDateOfBirth: LocalDate
  receiverPersons: {
    accountId: UUID
    receiverFirstName: string
    receiverLastName: string
  }[]
}

export interface ReceiverGroup {
  groupId: UUID
  groupName: string
  receivers: ReceiverChild[]
}

export const deserializeReceiverChild = (
  json: JsonOf<ReceiverChild>
): ReceiverChild => ({
  ...json,
  childDateOfBirth: LocalDate.parseIso(json.childDateOfBirth)
})
export interface ReceiverTriplet {
  unitId: UUID
  groupId?: UUID
  personId?: UUID
}

export interface BaseMessageAccount {
  id: UUID
  name: string
}
export interface MessageAccount extends BaseMessageAccount {
  personal: boolean
  daycareGroup?: {
    id: UUID
    name: string
    unitId: UUID
    unitName: string
  }
  unreadCount: number
}
export type GroupMessageAccount = Required<MessageAccount>
export const isGroupMessageAccount = (
  acc: MessageAccount
): acc is GroupMessageAccount => !!acc.daycareGroup

export interface Message {
  id: UUID
  senderId: UUID
  senderName: string
  sentAt: Date
  readAt: Date | null
  title: string
  content: string
  receivers: UUID[]
}

export interface MessageBody {
  title: string
  content: string
  type: MessageType
  recipientAccountIds: UUID[]
}

export type MessageType = 'MESSAGE' | 'BULLETIN'

export interface SentMessage {
  id: UUID
  type: MessageType
  threadTitle: string
  content: string
  recipients: BaseMessageAccount[]
  sentAt: Date
}
export const deserializeSentMessage = ({
  sentAt,
  ...rest
}: JsonOf<SentMessage>): SentMessage => ({
  ...rest,
  sentAt: new Date(sentAt)
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
  messages: json.messages.map((m) => ({
    ...m,
    sentAt: new Date(m.sentAt),
    readAt: m.readAt ? new Date(m.readAt) : null
  }))
})
