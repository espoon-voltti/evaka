// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { MessageAccount } from './types'

export type View = 'RECEIVED' | 'SENT' | 'RECEIVERS' | 'DRAFTS'

export interface AccountView {
  account: MessageAccount
  view: View
}

export const messageBoxes: View[] = ['RECEIVED', 'SENT', 'DRAFTS']
