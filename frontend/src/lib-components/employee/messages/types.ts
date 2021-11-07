// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Group,
  MessageReceiver,
  NestedMessageAccount,
  UpsertableDraftContent
} from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

export interface NestedGroupMessageAccount extends NestedMessageAccount {
  daycareGroup: Group
}

export const isNestedGroupMessageAccount = (
  nestedAccount: NestedMessageAccount
): nestedAccount is NestedGroupMessageAccount =>
  nestedAccount.account.type === 'GROUP' && nestedAccount.daycareGroup != null

export interface SaveDraftParams {
  accountId: UUID
  draftId: UUID
  content: UpsertableDraftContent
}

export const deserializeReceiver = (
  json: JsonOf<MessageReceiver>
): MessageReceiver => ({
  ...json,
  childDateOfBirth: LocalDate.parseIso(json.childDateOfBirth)
})
