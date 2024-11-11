// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { MessageAccount } from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'

const views = [
  'received',
  'sent',
  'drafts',
  'copies',
  'archive',
  'thread'
] as const
export type View = (typeof views)[number]

export const isValidView = (view: string): view is View =>
  views.some((v) => view === v)

export interface AccountView {
  account: MessageAccount
  view: View
  unitId: UUID | null
}

export const municipalMessageBoxes: View[] = ['sent', 'drafts']
export const serviceWorkerMessageBoxes: View[] = [
  'received',
  'sent',
  'drafts',
  'archive'
]
export const personalMessageBoxes: View[] = [
  'received',
  'sent',
  'drafts',
  'copies',
  'archive'
]
export const groupMessageBoxes: View[] = [
  'received',
  'sent',
  'drafts',
  'copies',
  'archive'
]
