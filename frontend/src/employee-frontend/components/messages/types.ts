// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from 'lib-common/json'
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
