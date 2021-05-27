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

export interface BaseMessageAccount {
  id: UUID
  name: string
}
interface AccountWithUnreadCount extends BaseMessageAccount {
  unreadCount: number
}
export interface PersonalMessageAccount extends AccountWithUnreadCount {
  type: 'PERSONAL'
}
export interface GroupMessageAccount extends AccountWithUnreadCount {
  type: 'GROUP'
  daycareGroup: {
    id: UUID
    name: string
    unitId: UUID
    unitName: string
  }
}
export type MessageAccount = PersonalMessageAccount | GroupMessageAccount

export const isGroupMessageAccount = (
  acc: MessageAccount
): acc is GroupMessageAccount => acc.type === 'GROUP'

export const isPersonalMessageAccount = (
  acc: MessageAccount
): acc is PersonalMessageAccount => acc.type === 'PERSONAL'

export type MessageType = 'MESSAGE' | 'BULLETIN'

export interface MessageBody {
  title: string
  content: string
  type: MessageType
  recipientAccountIds: UUID[]
  recipientNames: string[]
}

export interface UpsertableDraftContent {
  title: string
  content: string
  type: MessageType
  recipientIds: UUID[]
  recipientNames: string[]
}
export interface DraftContent extends UpsertableDraftContent {
  id: UUID
  created: Date
}
export const deserializeDraftContent = ({
  created,
  ...rest
}: JsonOf<DraftContent>): DraftContent => ({
  ...rest,
  created: new Date(created)
})

export interface SentMessage {
  id: UUID
  type: MessageType
  threadTitle: string
  content: string
  recipients: BaseMessageAccount[]
  recipientNames: string[]
  sentAt: Date
}
export const deserializeSentMessage = ({
  sentAt,
  ...rest
}: JsonOf<SentMessage>): SentMessage => ({
  ...rest,
  sentAt: new Date(sentAt)
})

export interface Message {
  id: UUID
  senderId: UUID
  senderName: string
  sentAt: Date
  readAt: Date | null
  content: string
}
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
