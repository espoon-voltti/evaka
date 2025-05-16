// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ChildBasicInfo } from 'lib-common/generated/api-types/incomestatement'
import type { IncomeStatement } from 'lib-common/generated/api-types/incomestatement'
import type { IncomeStatementId } from 'lib-common/generated/api-types/shared'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { PagedIncomeStatements } from 'lib-common/generated/api-types/incomestatement'
import type { PagedIncomeStatementsAwaitingHandler } from 'lib-common/generated/api-types/incomestatement'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { SearchIncomeStatementsRequest } from 'lib-common/generated/api-types/incomestatement'
import type { SetIncomeStatementHandledBody } from 'lib-common/generated/api-types/incomestatement'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonIncomeStatement } from 'lib-common/generated/api-types/incomestatement'
import { deserializeJsonPagedIncomeStatements } from 'lib-common/generated/api-types/incomestatement'
import { deserializeJsonPagedIncomeStatementsAwaitingHandler } from 'lib-common/generated/api-types/incomestatement'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementController.getIncomeStatement
*/
export async function getIncomeStatement(
  request: {
    incomeStatementId: IncomeStatementId
  }
): Promise<IncomeStatement> {
  const { data: json } = await client.request<JsonOf<IncomeStatement>>({
    url: uri`/employee/income-statements/${request.incomeStatementId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonIncomeStatement(json)
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementController.getIncomeStatementChildren
*/
export async function getIncomeStatementChildren(
  request: {
    guardianId: PersonId
  }
): Promise<ChildBasicInfo[]> {
  const { data: json } = await client.request<JsonOf<ChildBasicInfo[]>>({
    url: uri`/employee/income-statements/guardian/${request.guardianId}/children`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementController.getIncomeStatements
*/
export async function getIncomeStatements(
  request: {
    personId: PersonId,
    page: number
  }
): Promise<PagedIncomeStatements> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedIncomeStatements>>({
    url: uri`/employee/income-statements/person/${request.personId}`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonPagedIncomeStatements(json)
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementController.getIncomeStatementsAwaitingHandler
*/
export async function getIncomeStatementsAwaitingHandler(
  request: {
    body: SearchIncomeStatementsRequest
  }
): Promise<PagedIncomeStatementsAwaitingHandler> {
  const { data: json } = await client.request<JsonOf<PagedIncomeStatementsAwaitingHandler>>({
    url: uri`/employee/income-statements/awaiting-handler`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<SearchIncomeStatementsRequest>
  })
  return deserializeJsonPagedIncomeStatementsAwaitingHandler(json)
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementController.setIncomeStatementHandled
*/
export async function setIncomeStatementHandled(
  request: {
    incomeStatementId: IncomeStatementId,
    body: SetIncomeStatementHandledBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/income-statements/${request.incomeStatementId}/handled`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<SetIncomeStatementHandledBody>
  })
  return json
}
