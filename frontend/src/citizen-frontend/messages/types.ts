// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from 'lib-common/json'

export type ReceivedBulletin = {
  id: string
  sentAt: Date
  sender: string
  title: string
  content: string
  isRead: boolean
}

export const deserializeReceivedBulletin = (
  json: JsonOf<ReceivedBulletin>
): ReceivedBulletin => ({
  ...json,
  sentAt: new Date(json.sentAt)
})
