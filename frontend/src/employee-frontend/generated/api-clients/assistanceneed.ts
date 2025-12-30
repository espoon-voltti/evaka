// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { AssistanceNeedVoucherCoefficient } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedVoucherCoefficientId } from 'lib-common/generated/api-types/shared'
import type { AssistanceNeedVoucherCoefficientRequest } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedVoucherCoefficientResponse } from 'lib-common/generated/api-types/assistanceneed'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import { client } from '../../api/client'
import { deserializeJsonAssistanceNeedVoucherCoefficient } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonAssistanceNeedVoucherCoefficientResponse } from 'lib-common/generated/api-types/assistanceneed'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController.createAssistanceNeedVoucherCoefficient
*/
export async function createAssistanceNeedVoucherCoefficient(
  request: {
    childId: PersonId,
    body: AssistanceNeedVoucherCoefficientRequest
  }
): Promise<AssistanceNeedVoucherCoefficient> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedVoucherCoefficient>>({
    url: uri`/employee/children/${request.childId}/assistance-need-voucher-coefficients`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AssistanceNeedVoucherCoefficientRequest>
  })
  return deserializeJsonAssistanceNeedVoucherCoefficient(json)
}


/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController.deleteAssistanceNeedVoucherCoefficient
*/
export async function deleteAssistanceNeedVoucherCoefficient(
  request: {
    id: AssistanceNeedVoucherCoefficientId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-voucher-coefficients/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController.getAssistanceNeedVoucherCoefficients
*/
export async function getAssistanceNeedVoucherCoefficients(
  request: {
    childId: PersonId
  }
): Promise<AssistanceNeedVoucherCoefficientResponse[]> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedVoucherCoefficientResponse[]>>({
    url: uri`/employee/children/${request.childId}/assistance-need-voucher-coefficients`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAssistanceNeedVoucherCoefficientResponse(e))
}


/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController.updateAssistanceNeedVoucherCoefficient
*/
export async function updateAssistanceNeedVoucherCoefficient(
  request: {
    id: AssistanceNeedVoucherCoefficientId,
    body: AssistanceNeedVoucherCoefficientRequest
  }
): Promise<AssistanceNeedVoucherCoefficient> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedVoucherCoefficient>>({
    url: uri`/employee/assistance-need-voucher-coefficients/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<AssistanceNeedVoucherCoefficientRequest>
  })
  return deserializeJsonAssistanceNeedVoucherCoefficient(json)
}
