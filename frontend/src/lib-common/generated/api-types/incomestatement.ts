// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import { ProviderType } from './daycare'
import { SortDirection } from './invoicing'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.incomestatement.ChildBasicInfo
*/
export interface ChildBasicInfo {
  firstName: string
  id: UUID
  lastName: string
}

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementAwaitingHandler
*/
export interface IncomeStatementAwaitingHandler {
  created: HelsinkiDateTime
  id: UUID
  personId: UUID
  personName: string
  primaryCareArea: string | null
  startDate: LocalDate
  type: IncomeStatementType
}

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementSortParam
*/
export type IncomeStatementSortParam =
  | 'CREATED'
  | 'START_DATE'

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementType
*/
export type IncomeStatementType =
  | 'HIGHEST_FEE'
  | 'INCOME'
  | 'CHILD_INCOME'

/**
* Generated from fi.espoo.evaka.incomestatement.SearchIncomeStatementsRequest
*/
export interface SearchIncomeStatementsRequest {
  areas: string[] | null
  page: number
  pageSize: number
  providerTypes: ProviderType[] | null
  sentEndDate: LocalDate | null
  sentStartDate: LocalDate | null
  sortBy: IncomeStatementSortParam | null
  sortDirection: SortDirection | null
}

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementController.SetIncomeStatementHandledBody
*/
export interface SetIncomeStatementHandledBody {
  handled: boolean
  handlerNote: string
}
