// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getIncomeStatementsAwaitingHandler } from 'employee-frontend/generated/api-clients/incomestatement'
import { createQueryKeys } from 'employee-frontend/query'
import { SearchIncomeStatementsRequest } from 'lib-common/generated/api-types/incomestatement'
import { query } from 'lib-common/query'

const queryKeys = createQueryKeys('incomeStatements', {
  incomeStatementsAwaitingHandler: (params: SearchIncomeStatementsRequest) => [
    'incomeStatementsAwaitingHandler',
    params
  ]
})

export const incomeStatementsAwaitingHandlerQuery = query({
  api: getIncomeStatementsAwaitingHandler,
  queryKey: (params) => queryKeys.incomeStatementsAwaitingHandler(params.body)
})
