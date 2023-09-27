// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Entrepreneur,
  EstimatedIncome,
  IncomeStatement,
  IncomeStatementAwaitingHandler,
  SelfEmployed
} from 'lib-common/generated/api-types/incomestatement'
import LocalDate from 'lib-common/local-date'

import HelsinkiDateTime from '../helsinki-date-time'
import { JsonOf } from '../json'

export function deserializeIncomeStatement(
  data: JsonOf<IncomeStatement>
): IncomeStatement {
  const startDate = LocalDate.parseIso(data.startDate)
  const endDate = data.endDate ? LocalDate.parseIso(data.endDate) : null
  const created = HelsinkiDateTime.parseIso(data.created)
  const updated = HelsinkiDateTime.parseIso(data.updated)
  switch (data.type) {
    case 'HIGHEST_FEE':
      return { ...data, startDate, endDate, created, updated }
    case 'CHILD_INCOME':
      return { ...data, startDate, endDate, created, updated }
    case 'INCOME':
      return {
        ...data,
        startDate,
        endDate,
        entrepreneur: deserializeEntrepreneur(data.entrepreneur),
        created,
        updated
      }
  }
}

function deserializeEntrepreneur(
  entrepreneur: JsonOf<Entrepreneur> | null
): Entrepreneur | null {
  if (!entrepreneur) return null
  return {
    ...entrepreneur,
    startOfEntrepreneurship: LocalDate.parseIso(
      entrepreneur.startOfEntrepreneurship
    ),
    selfEmployed: deserializeSelfEmployed(entrepreneur.selfEmployed)
  }
}

function deserializeSelfEmployed(
  selfEmployed: JsonOf<SelfEmployed> | null
): SelfEmployed | null {
  if (!selfEmployed) return null
  return {
    ...selfEmployed,
    estimatedIncome: deserializeEstimatedIncome(selfEmployed.estimatedIncome)
  }
}

function deserializeEstimatedIncome(
  estimatedIncome: JsonOf<EstimatedIncome> | null
): EstimatedIncome | null {
  if (!estimatedIncome) return null
  return {
    ...estimatedIncome,
    incomeStartDate: LocalDate.parseIso(estimatedIncome.incomeStartDate),
    incomeEndDate: estimatedIncome.incomeEndDate
      ? LocalDate.parseIso(estimatedIncome.incomeEndDate)
      : null
  }
}

export const deserializeIncomeStatementAwaitingHandler = (
  data: JsonOf<IncomeStatementAwaitingHandler>
): IncomeStatementAwaitingHandler => ({
  ...data,
  startDate: LocalDate.parseIso(data.startDate),
  created: HelsinkiDateTime.parseIso(data.created)
})
