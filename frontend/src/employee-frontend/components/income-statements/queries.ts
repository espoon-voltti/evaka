// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'
import { Arg0 } from 'lib-common/types'

import { getIncomeStatementsAwaitingHandler } from '../../generated/api-clients/incomestatement'
import { createQueryKeys } from '../../query'

export const queryKeys = createQueryKeys('incomeStatements', {
  incomeStatementsAwaitingHandler: (
    arg: Arg0<typeof getIncomeStatementsAwaitingHandler>
  ) => ['incomeStatementsAwaitingHandler', arg]
})

export const incomeStatementsAwaitingHandlerQuery = query({
  api: (arg: Arg0<typeof getIncomeStatementsAwaitingHandler>) =>
    getIncomeStatementsAwaitingHandler(arg),
  queryKey: queryKeys.incomeStatementsAwaitingHandler
})
