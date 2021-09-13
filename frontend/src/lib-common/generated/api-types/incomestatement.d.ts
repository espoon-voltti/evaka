// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementAwaitingHandler
*/
export interface IncomeStatementAwaitingHandler {
    id: UUID
    personId: UUID
    personName: string
    type: IncomeStatementType
}

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementType
*/
export type IncomeStatementType = 'HIGHEST_FEE' | 'INCOME'