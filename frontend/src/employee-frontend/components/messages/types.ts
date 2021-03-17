// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from '../../types'

export type Bulletin = {
  id: UUID
  title: string
  content: string
  createdByEmployee: UUID
  createdByEmployeeName: string
  groupId: UUID | null
  groupName: string | null
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
  receivers: {
    receiverId: UUID
    receiverFirstName: string
    receiverLastName: string
  }[]
}

export interface ReceiverGroup {
  groupId: UUID
  groupName: string
  receiverChildren: ReceiverChild[]
}
export const deserializeReceiverChild = (
  json: JsonOf<ReceiverChild>
): ReceiverChild => ({
  ...json,
  childDateOfBirth: LocalDate.parseIso(json.childDateOfBirth)
})
