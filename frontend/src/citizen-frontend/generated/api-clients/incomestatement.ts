// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ChildBasicInfo } from 'lib-common/generated/api-types/incomestatement'
import type { IncomeStatement } from 'lib-common/generated/api-types/incomestatement'
import type { IncomeStatementBody } from 'lib-common/generated/api-types/incomestatement'
import type { IncomeStatementId } from 'lib-common/generated/api-types/shared'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { PagedIncomeStatements } from 'lib-common/generated/api-types/incomestatement'
import type { PartnerIncomeStatementStatusResponse } from 'lib-common/generated/api-types/incomestatement'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { UpdateSentIncomeStatementBody } from 'lib-common/generated/api-types/incomestatement'
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
    childId: PersonId,
    draft?: boolean | null,
    body: IncomeStatementBody
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['draft', request.draft?.toString()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/income-statements/child/${request.childId}`.toString(),
    method: 'POST',
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
    draft: boolean,
    body: IncomeStatementBody
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['draft', request.draft.toString()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/income-statements`.toString(),
    method: 'POST',
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
    id: IncomeStatementId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/income-statements/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.getChildIncomeStatementStartDates
*/
export async function getChildIncomeStatementStartDates(
  request: {
    childId: PersonId
  }
): Promise<LocalDate[]> {
  const { data: json } = await client.request<JsonOf<LocalDate[]>>({
    url: uri`/citizen/income-statements/child/start-dates/${request.childId}`.toString(),
    method: 'GET'
  })
  return json.map(e => LocalDate.parseIso(e))
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.getChildIncomeStatements
*/
export async function getChildIncomeStatements(
  request: {
    childId: PersonId,
    page: number
  }
): Promise<PagedIncomeStatements> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedIncomeStatements>>({
    url: uri`/citizen/income-statements/child/${request.childId}`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonPagedIncomeStatements(json)
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.getIncomeStatement
*/
export async function getIncomeStatement(
  request: {
    incomeStatementId: IncomeStatementId
  }
): Promise<IncomeStatement> {
  const { data: json } = await client.request<JsonOf<IncomeStatement>>({
    url: uri`/citizen/income-statements/${request.incomeStatementId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonIncomeStatement(json)
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.getIncomeStatementChildren
*/
export async function getIncomeStatementChildren(): Promise<ChildBasicInfo[]> {
  const { data: json } = await client.request<JsonOf<ChildBasicInfo[]>>({
    url: uri`/citizen/income-statements/children`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.getIncomeStatementStartDates
*/
export async function getIncomeStatementStartDates(): Promise<LocalDate[]> {
  const { data: json } = await client.request<JsonOf<LocalDate[]>>({
    url: uri`/citizen/income-statements/start-dates/`.toString(),
    method: 'GET'
  })
  return json.map(e => LocalDate.parseIso(e))
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.getIncomeStatements
*/
export async function getIncomeStatements(
  request: {
    page: number
  }
): Promise<PagedIncomeStatements> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedIncomeStatements>>({
    url: uri`/citizen/income-statements`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonPagedIncomeStatements(json)
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.getPartnerIncomeStatementStatus
*/
export async function getPartnerIncomeStatementStatus(): Promise<PartnerIncomeStatementStatusResponse> {
  const { data: json } = await client.request<JsonOf<PartnerIncomeStatementStatusResponse>>({
    url: uri`/citizen/income-statements/partner`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.updateIncomeStatement
*/
export async function updateIncomeStatement(
  request: {
    incomeStatementId: IncomeStatementId,
    draft: boolean,
    body: IncomeStatementBody
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['draft', request.draft.toString()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/income-statements/${request.incomeStatementId}`.toString(),
    method: 'PUT',
    params,
    data: request.body satisfies JsonCompatible<IncomeStatementBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.updateSentIncomeStatement
*/
export async function updateSentIncomeStatement(
  request: {
    incomeStatementId: IncomeStatementId,
    body: UpdateSentIncomeStatementBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/income-statements/${request.incomeStatementId}/update-sent`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<UpdateSentIncomeStatementBody>
  })
  return json
}
