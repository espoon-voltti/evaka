// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  MessageAccount,
  MessageThreadFolder
} from 'lib-common/generated/api-types/messaging'
import type { UUID } from 'lib-common/types'

const views = [
  'received',
  'sent',
  'drafts',
  'copies',
  'archive',
  'thread'
] as const

export type StandardView = (typeof views)[number]

export type View = StandardView | MessageThreadFolder

export const isStandardView = (
  view: MessageThreadFolder | string
): view is StandardView => views.some((v) => view === v)

export const isFolderView = (
  view: MessageThreadFolder | string
): view is MessageThreadFolder => !isStandardView(view)

export interface AccountView {
  account: MessageAccount
  view: View
  unitId: UUID | null
}

export const municipalMessageBoxes: StandardView[] = ['sent', 'drafts']
export const serviceWorkerMessageBoxes: StandardView[] = [
  'received',
  'sent',
  'drafts',
  'archive'
]
export const financeMessageBoxes: StandardView[] = [
  'received',
  'sent',
  'drafts',
  'archive'
]
export const personalMessageBoxes: StandardView[] = [
  'received',
  'sent',
  'drafts',
  'copies',
  'archive'
]
export const groupMessageBoxes: StandardView[] = [
  'received',
  'sent',
  'drafts',
  'copies',
  'archive'
]
