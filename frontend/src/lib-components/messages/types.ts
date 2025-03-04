// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Group,
  AuthorizedMessageAccount,
  UpdatableDraftContent
} from 'lib-common/generated/api-types/messaging'
import {
  MessageAccountId,
  MessageDraftId
} from 'lib-common/generated/api-types/shared'

export interface GroupMessageAccount extends AuthorizedMessageAccount {
  daycareGroup: Group
}

export const isGroupMessageAccount = (
  acc: AuthorizedMessageAccount
): acc is GroupMessageAccount =>
  acc.account.type === 'GROUP' && acc.daycareGroup !== null

export const isPersonalMessageAccount = (
  acc: AuthorizedMessageAccount
): acc is AuthorizedMessageAccount => acc.account.type === 'PERSONAL'

export const isMunicipalMessageAccount = (
  acc: AuthorizedMessageAccount
): acc is AuthorizedMessageAccount => acc.account.type === 'MUNICIPAL'

export const isServiceWorkerMessageAccount = (
  acc: AuthorizedMessageAccount
): acc is AuthorizedMessageAccount => acc.account.type === 'SERVICE_WORKER'

export const isFinanceMessageAccount = (
  acc: AuthorizedMessageAccount
): acc is AuthorizedMessageAccount => acc.account.type === 'FINANCE'

export interface SaveDraftParams {
  accountId: MessageAccountId
  draftId: MessageDraftId
  content: UpdatableDraftContent
}
