// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from '../../types'

export type Bulletin = {
  id: UUID
  sender: string
  title: string
  content: string
  createdByEmployee: UUID
  createdByEmployeeName: string
  receiverUnits: { unitId: UUID; unitName: string }[]
  receiverGroups: { unitId: UUID; groupId: UUID; groupName: string }[]
  receiverChildren: { childId: UUID; firstName: string; lastName: string }[]
  sentAt: Date | null
}

export type Recipient = {
  personId: string
  firstName: string
  lastName: string
  guardian: boolean
  headOfChild: boolean
  blocklisted: boolean
}

export function deserializeBulletin(json: JsonOf<Bulletin>): Bulletin {
  return {
    ...json,
    sentAt: json.sentAt ? new Date(json.sentAt) : null
  }
}

export type IdAndName = {
  id: UUID
  name: string
}

export type SentBulletin = Bulletin & {
  sentAt: Date
}

export const deserializeSentBulletin = (
  json: JsonOf<SentBulletin>
): SentBulletin => ({
  ...json,
  sentAt: new Date(json.sentAt)
})

export interface ReceiverChild {
  childId: UUID
  childFirstName: string
  childLastName: string
  childDateOfBirth: LocalDate
  receiverPersons: {
    receiverId: UUID
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

export interface EnrichedMessageAccount {
  accountId: UUID
  accountName: string
  groupId: UUID | undefined
  groupName: string | undefined
  unitId: UUID | undefined
  unitName: string | undefined
  personal: boolean
}


interface Message {
  id: UUID
  senderId: UUID
  senderName: string
  sentAt: Date
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
  messages: json.messages.map((m) => ({ ...m, sentAt: new Date(m.sentAt) }))
})
