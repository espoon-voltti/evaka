// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getIncomeStatementsAwaitingHandler } from '../../generated/api-clients/incomestatement'

const q = new Queries()

export const incomeStatementsAwaitingHandlerQuery = q.query(
  getIncomeStatementsAwaitingHandler
)
