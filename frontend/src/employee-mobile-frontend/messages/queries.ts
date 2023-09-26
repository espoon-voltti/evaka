// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'

import { createQueryKeys } from '../query'

import { getMessagingAccounts } from './api'

const queryKeys = createQueryKeys('messages', {
  accounts: (unitId: string) => ['accounts', unitId]
})

export const messagingAccountsQuery = query({
  api: getMessagingAccounts,
  queryKey: queryKeys.accounts
})
