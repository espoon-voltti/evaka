// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { AxiosHeaders } from 'axios'
import { ChildBasicInfo } from 'lib-common/generated/api-types/incomestatement'
import { IncomeStatement } from 'lib-common/generated/api-types/incomestatement'
import { IncomeStatementBody } from 'lib-common/generated/api-types/incomestatement'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PagedIncomeStatements } from 'lib-common/generated/api-types/incomestatement'
import { UUID } from 'lib-common/types'
import { client } from '../../api-client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonIncomeStatement } from 'lib-common/generated/api-types/incomestatement'
import { deserializeJsonPagedIncomeStatements } from 'lib-common/generated/api-types/incomestatement'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.createChildIncomeStatement
*/
export async function createChildIncomeStatement(
  request: {
    childId: UUID,
    draft?: boolean | null,
    body: IncomeStatementBody
  },
  headers?: AxiosHeaders
): Promise<void> {
  const params = createUrlSearchParams(
    ['draft', request.draft?.toString()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/income-statements/child/${request.childId}`.toString(),
    method: 'POST',
    headers,
    params,
    data: request.body satisfies JsonCompatible<IncomeStatementBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.createIncomeStatement
*/
export async function createIncomeStatement(
  request: {
    draft?: boolean | null,
    body: IncomeStatementBody
  },
  headers?: AxiosHeaders
): Promise<void> {
  const params = createUrlSearchParams(
    ['draft', request.draft?.toString()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/income-statements`.toString(),
    method: 'POST',
    headers,
    params,
    data: request.body satisfies JsonCompatible<IncomeStatementBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.deleteIncomeStatement
*/
export async function deleteIncomeStatement(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/income-statements/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.getChildIncomeStatement
*/
export async function getChildIncomeStatement(
  request: {
    childId: UUID,
    incomeStatementId: UUID
  },
  headers?: AxiosHeaders
): Promise<IncomeStatement> {
  const { data: json } = await client.request<JsonOf<IncomeStatement>>({
    url: uri`/citizen/income-statements/child/${request.childId}/${request.incomeStatementId}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonIncomeStatement(json)
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.getChildIncomeStatementStartDates
*/
export async function getChildIncomeStatementStartDates(
  request: {
    childId: UUID
  },
  headers?: AxiosHeaders
): Promise<LocalDate[]> {
  const { data: json } = await client.request<JsonOf<LocalDate[]>>({
    url: uri`/citizen/income-statements/child/start-dates/${request.childId}`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => LocalDate.parseIso(e))
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.getChildIncomeStatements
*/
export async function getChildIncomeStatements(
  request: {
    childId: UUID,
    page: number
  },
  headers?: AxiosHeaders
): Promise<PagedIncomeStatements> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedIncomeStatements>>({
    url: uri`/citizen/income-statements/child/${request.childId}`.toString(),
    method: 'GET',
    headers,
    params
  })
  return deserializeJsonPagedIncomeStatements(json)
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.getIncomeStatement
*/
export async function getIncomeStatement(
  request: {
    incomeStatementId: UUID
  },
  headers?: AxiosHeaders
): Promise<IncomeStatement> {
  const { data: json } = await client.request<JsonOf<IncomeStatement>>({
    url: uri`/citizen/income-statements/${request.incomeStatementId}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonIncomeStatement(json)
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.getIncomeStatementChildren
*/
export async function getIncomeStatementChildren(
  headers?: AxiosHeaders
): Promise<ChildBasicInfo[]> {
  const { data: json } = await client.request<JsonOf<ChildBasicInfo[]>>({
    url: uri`/citizen/income-statements/children`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.getIncomeStatementStartDates
*/
export async function getIncomeStatementStartDates(
  headers?: AxiosHeaders
): Promise<LocalDate[]> {
  const { data: json } = await client.request<JsonOf<LocalDate[]>>({
    url: uri`/citizen/income-statements/start-dates/`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => LocalDate.parseIso(e))
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.getIncomeStatements
*/
export async function getIncomeStatements(
  request: {
    page: number
  },
  headers?: AxiosHeaders
): Promise<PagedIncomeStatements> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedIncomeStatements>>({
    url: uri`/citizen/income-statements`.toString(),
    method: 'GET',
    headers,
    params
  })
  return deserializeJsonPagedIncomeStatements(json)
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.removeChildIncomeStatement
*/
export async function removeChildIncomeStatement(
  request: {
    childId: UUID,
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/income-statements/child/${request.childId}/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.updateChildIncomeStatement
*/
export async function updateChildIncomeStatement(
  request: {
    childId: UUID,
    incomeStatementId: UUID,
    draft?: boolean | null,
    body: IncomeStatementBody
  },
  headers?: AxiosHeaders
): Promise<void> {
  const params = createUrlSearchParams(
    ['draft', request.draft?.toString()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/income-statements/child/${request.childId}/${request.incomeStatementId}`.toString(),
    method: 'PUT',
    headers,
    params,
    data: request.body satisfies JsonCompatible<IncomeStatementBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.updateIncomeStatement
*/
export async function updateIncomeStatement(
  request: {
    incomeStatementId: UUID,
    draft?: boolean | null,
    body: IncomeStatementBody
  },
  headers?: AxiosHeaders
): Promise<void> {
  const params = createUrlSearchParams(
    ['draft', request.draft?.toString()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/income-statements/${request.incomeStatementId}`.toString(),
    method: 'PUT',
    headers,
    params,
    data: request.body satisfies JsonCompatible<IncomeStatementBody>
  })
  return json
}
