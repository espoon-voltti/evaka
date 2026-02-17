// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getSelectableFinanceDecisionHandlers } from '../../generated/api-clients/invoicing'

const q = new Queries()

export const selectableFinanceDecisionHandlersQuery = q.query(
  getSelectableFinanceDecisionHandlers
)
