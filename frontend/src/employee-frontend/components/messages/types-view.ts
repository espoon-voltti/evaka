// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { MessageAccount } from 'lib-common/generated/api-types/messaging'

export type View = 'RECEIVED' | 'SENT' | 'DRAFTS' | 'COPIES' | 'ARCHIVE'

export interface AccountView {
  account: MessageAccount
  view: View
}

export const municipalMessageBoxes: View[] = ['SENT', 'DRAFTS']
export const personalMessageBoxes: View[] = [
  'RECEIVED',
  'SENT',
  'DRAFTS',
  'ARCHIVE'
]
export const groupMessageBoxes: View[] = [
  'RECEIVED',
  'SENT',
  'DRAFTS',
  'COPIES',
  'ARCHIVE'
]
