// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import partition from 'lodash/partition'

import type { Response, Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import type { Income, IncomeOption, IncomeBody } from '../types/income'

import { client } from './client'

export async function getIncomes(personId: UUID): Promise<Result<Income[]>> {
  return client
    .get<JsonOf<Response<Income[]>>>(`/incomes?personId=${personId}`)
    .then((res) => res.data.data)
    .then((incomes) =>
      incomes.map((income) => ({
        ...income,
        validFrom: LocalDate.parseIso(income.validFrom),
        validTo: income.validTo
          ? LocalDate.parseIso(income.validTo)
          : undefined,
        updatedAt: new Date(income.updatedAt)
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function createIncome(
  personId: UUID,
  income: IncomeBody
): Promise<Result<string>> {
  return client
    .post<string>('/incomes', { personId, ...income })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function updateIncome(
  incomeId: UUID,
  income: Income
): Promise<Result<void>> {
  return client
    .put<void>(`/incomes/${incomeId}`, income)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function deleteIncome(incomeId: string): Promise<Result<void>> {
  return client
    .delete<void>(`/incomes/${incomeId}`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export type IncomeTypeOptions = {
  incomeTypes: IncomeOption[]
  expenseTypes: IncomeOption[]
}

export async function getIncomeOptions(): Promise<Result<IncomeTypeOptions>> {
  return client
    .get<IncomeTypes>(`/incomes/types`)
    .then((res) =>
      Success.of(
        partition(
          Object.entries(res.data).map(([value, type]) => ({ ...type, value })),
          (type) => type.multiplier > 0
        )
      ).map(([incomeTypes, expenseTypes]) => ({ incomeTypes, expenseTypes }))
    )
    .catch((e) => Failure.fromError(e))
}

type IncomeTypes = Record<string, IncomeType>

interface IncomeType {
  nameFi: string
  multiplier: number
  withCoefficient: boolean
  isSubType: boolean
}
