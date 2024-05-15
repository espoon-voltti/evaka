// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AssistanceNeedVoucherCoefficient } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedVoucherCoefficientRequest } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedVoucherCoefficientResponse } from 'lib-common/generated/api-types/assistanceneed'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../client'
import { deserializeJsonAssistanceNeedVoucherCoefficient } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonAssistanceNeedVoucherCoefficientResponse } from 'lib-common/generated/api-types/assistanceneed'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController.createAssistanceNeedVoucherCoefficient
*/
export async function createAssistanceNeedVoucherCoefficient(
  request: {
    childId: UUID,
    body: AssistanceNeedVoucherCoefficientRequest
  }
): Promise<AssistanceNeedVoucherCoefficient> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedVoucherCoefficient>>({
    url: uri`/children/${request.childId}/assistance-need-voucher-coefficients`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AssistanceNeedVoucherCoefficientRequest>
  })
  return deserializeJsonAssistanceNeedVoucherCoefficient(json)
}


/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController.getAssistanceNeedVoucherCoefficients
*/
export async function getAssistanceNeedVoucherCoefficients(
  request: {
    childId: UUID
  }
): Promise<AssistanceNeedVoucherCoefficientResponse[]> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedVoucherCoefficientResponse[]>>({
    url: uri`/children/${request.childId}/assistance-need-voucher-coefficients`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAssistanceNeedVoucherCoefficientResponse(e))
}
