// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { ChildBasicInfo } from 'lib-common/generated/api-types/incomestatement'
import { IncomeStatement } from 'lib-common/generated/api-types/incomestatement'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PagedIncomeStatements } from 'lib-common/generated/api-types/incomestatement'
import { PagedIncomeStatementsAwaitingHandler } from 'lib-common/generated/api-types/incomestatement'
import { SearchIncomeStatementsRequest } from 'lib-common/generated/api-types/incomestatement'
import { SetIncomeStatementHandledBody } from 'lib-common/generated/api-types/incomestatement'
import { UUID } from 'lib-common/types'
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
    personId: UUID,
    incomeStatementId: UUID
  }
): Promise<IncomeStatement> {
  const { data: json } = await client.request<JsonOf<IncomeStatement>>({
    url: uri`/employee/income-statements/person/${request.personId}/${request.incomeStatementId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonIncomeStatement(json)
}


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementController.getIncomeStatementChildren
*/
export async function getIncomeStatementChildren(
  request: {
    guardianId: UUID
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
    personId: UUID,
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
    incomeStatementId: UUID,
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
