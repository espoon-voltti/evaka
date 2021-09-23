// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import {
  DraftContent,
  Group,
  MessageReceiver,
  NestedMessageAccount,
  SentMessage
} from 'lib-common/generated/api-types/messaging'

export const deserializeReceiver = (
  json: JsonOf<MessageReceiver>
): MessageReceiver => ({
  ...json,
  childDateOfBirth: LocalDate.parseIso(json.childDateOfBirth)
})

export interface NestedGroupMessageAccount extends NestedMessageAccount {
  daycareGroup: Group
}

export const isNestedGroupMessageAccount = (
  nestedAccount: NestedMessageAccount
): nestedAccount is NestedGroupMessageAccount =>
  nestedAccount.account.type === 'GROUP' && nestedAccount.daycareGroup != null

export const deserializeDraftContent = ({
  created,
  ...rest
}: JsonOf<DraftContent>): DraftContent => ({
  ...rest,
  created: new Date(created)
})

export const deserializeSentMessage = ({
  sentAt,
  ...rest
}: JsonOf<SentMessage>): SentMessage => ({
  ...rest,
  sentAt: new Date(sentAt)
})
