// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { deserializeIncomeStatement } from 'lib-common/api-types/incomeStatement'
import {
  ChildBasicInfo,
  IncomeStatement,
  PagedIncomeStatements
} from 'lib-common/generated/api-types/incomestatement'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../api-client'

import { IncomeStatementBody } from './types/body'

export async function getIncomeStatements(
  page: number,
  pageSize: number
): Promise<PagedIncomeStatements> {
  return client
    .get<JsonOf<PagedIncomeStatements>>('/citizen/income-statements', {
      params: { page, pageSize }
    })
    .then(({ data: { data, ...rest } }) => ({
      ...rest,
      data: data.map(deserializeIncomeStatement)
    }))
}

export async function getChildIncomeStatements(
  childId: string,
  page: number,
  pageSize: number
): Promise<PagedIncomeStatements> {
  return client
    .get<JsonOf<PagedIncomeStatements>>(
      `/citizen/income-statements/child/${childId}`,
      {
        params: { page, pageSize }
      }
    )
    .then(({ data: { data, ...rest } }) => ({
      ...rest,
      data: data.map(deserializeIncomeStatement)
    }))
}

export async function getChildIncomeStatement({
  childId,
  id
}: {
  childId: UUID
  id: UUID
}): Promise<IncomeStatement> {
  return client
    .get<
      JsonOf<IncomeStatement>
    >(`/citizen/income-statements/child/${childId}/${id}`)
    .then((res) => deserializeIncomeStatement(res.data))
}

export async function getIncomeStatement(id: UUID): Promise<IncomeStatement> {
  return client
    .get<JsonOf<IncomeStatement>>(`/citizen/income-statements/${id}`)
    .then((res) => deserializeIncomeStatement(res.data))
}

export const getIncomeStatementStartDates = (): Promise<LocalDate[]> =>
  client
    .get<JsonOf<LocalDate[]>>(`/citizen/income-statements/start-dates/`)
    .then(({ data }) => data.map((d) => LocalDate.parseIso(d)))

export const getChildIncomeStatementStartDates = (
  childId: string
): Promise<LocalDate[]> =>
  client
    .get<
      JsonOf<LocalDate[]>
    >(`/citizen/income-statements/child/start-dates/${childId}`)
    .then(({ data }) => data.map((d) => LocalDate.parseIso(d)))

export async function createIncomeStatement(
  body: IncomeStatementBody
): Promise<void> {
  return client.post('/citizen/income-statements', body)
}

export async function createChildIncomeStatement({
  childId,
  body
}: {
  childId: string
  body: IncomeStatementBody
}): Promise<void> {
  return client.post(`/citizen/income-statements/child/${childId}`, body)
}

export async function updateIncomeStatement({
  id,
  body
}: {
  id: UUID
  body: IncomeStatementBody
}): Promise<void> {
  return client.put(`/citizen/income-statements/${id}`, body)
}

export async function updateChildIncomeStatement({
  childId,
  id,
  body
}: {
  childId: UUID
  id: UUID
  body: IncomeStatementBody
}): Promise<void> {
  return client.put(`/citizen/income-statements/child/${childId}/${id}`, body)
}

export async function deleteIncomeStatement(id: UUID): Promise<void> {
  return client.delete(`/citizen/income-statements/${id}`)
}

export async function deleteChildIncomeStatement({
  childId,
  id
}: {
  childId: UUID
  id: UUID
}): Promise<void> {
  return client.delete(`/citizen/income-statements/child/${childId}/${id}`)
}

export function getGuardianIncomeStatementChildren(): Promise<
  ChildBasicInfo[]
> {
  return client
    .get<JsonOf<ChildBasicInfo[]>>('/citizen/income-statements/children')
    .then((res) => res.data)
}
