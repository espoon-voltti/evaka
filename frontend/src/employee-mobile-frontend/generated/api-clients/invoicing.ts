// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { Employee } from 'lib-common/generated/api-types/pis'
import { GenerateDecisionsBody } from 'lib-common/generated/api-types/invoicing'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { client } from '../../client'
import { deserializeJsonEmployee } from 'lib-common/generated/api-types/pis'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionGeneratorController.generateDecisions
*/
export async function generateDecisions(
  request: {
    body: GenerateDecisionsBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/fee-decision-generator/generate`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<GenerateDecisionsBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceDecisionController.getSelectableFinanceDecisionHandlers
*/
export async function getSelectableFinanceDecisionHandlers(): Promise<Employee[]> {
  const { data: json } = await client.request<JsonOf<Employee[]>>({
    url: uri`/finance-decisions/selectable-handlers`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonEmployee(e))
}
