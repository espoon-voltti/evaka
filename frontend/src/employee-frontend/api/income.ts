// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import partition from 'lodash/partition'

import { Failure, Response, Result, Success } from 'lib-common/api'
import { IncomeCoefficient } from 'lib-common/api-types/income'
import { IncomeNotification } from 'lib-common/generated/api-types/invoicing'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import {
  Income,
  IncomeOption,
  IncomeBody,
  IncomeWithPermittedActions
} from '../types/income'

import { client } from './client'

export async function getIncomes(
  personId: UUID
): Promise<Result<IncomeWithPermittedActions[]>> {
  return client
    .get<JsonOf<Response<IncomeWithPermittedActions[]>>>(
      `/incomes?personId=${personId}`
    )
    .then((res) => res.data.data)
    .then((incomes) =>
      incomes.map(({ data: income, permittedActions }) => ({
        data: {
          ...income,
          validFrom: LocalDate.parseIso(income.validFrom),
          validTo: income.validTo
            ? LocalDate.parseIso(income.validTo)
            : undefined,
          updatedAt: HelsinkiDateTime.parseIso(income.updatedAt)
        },
        permittedActions
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

export type IncomeCoefficientMultipliers = Record<IncomeCoefficient, number>

export async function getIncomeCoefficientMultipliers(): Promise<IncomeCoefficientMultipliers> {
  return client
    .get<IncomeCoefficientMultipliers>(`/incomes/multipliers`)
    .then((res) => res.data)
}

export async function getIncomeNotifications(
  personId: UUID
): Promise<Result<IncomeNotification[]>> {
  return client
    .get<JsonOf<Response<IncomeNotification[]>>>(
      `/incomes/notifications?personId=${personId}`
    )
    .then((res) => res.data.data)
    .then((incomeNotifications) =>
      incomeNotifications.map((incomeNotification) => ({
        ...incomeNotification,
        created: HelsinkiDateTime.parseIso(incomeNotification.created)
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}
