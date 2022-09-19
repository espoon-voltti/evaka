// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { MessageAccount } from 'lib-common/generated/api-types/messaging'

export type View = 'RECEIVED' | 'SENT' | 'DRAFTS' | 'COPIES'

export interface AccountView {
  account: MessageAccount
  view: View
}

export const personalMessageBoxes: View[] = ['RECEIVED', 'SENT', 'DRAFTS']
export const groupMessageBoxes: View[] = [
  'RECEIVED',
  'SENT',
  'DRAFTS',
  'COPIES'
]
