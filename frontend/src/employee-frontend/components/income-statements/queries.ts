// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  getIncomeStatement,
  getIncomeStatementsAwaitingHandler,
  setIncomeStatementHandled
} from '../../generated/api-clients/incomestatement'

const q = new Queries()

export const incomeStatementQuery = q.query(getIncomeStatement)

export const incomeStatementsAwaitingHandlerQuery = q.query(
  getIncomeStatementsAwaitingHandler
)

export const setIncomeStatementHandledMutation = q.mutation(
  setIncomeStatementHandled,
  [
    ({ incomeStatementId }) => incomeStatementQuery({ incomeStatementId }),
    incomeStatementsAwaitingHandlerQuery.prefix
  ]
)
