// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'employee-frontend/api/client'
import type { Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import type {
  AssistanceNeedVoucherCoefficientRequest,
  AssistanceNeedVoucherCoefficientResponse
} from 'lib-common/generated/api-types/assistanceneed'
import type { JsonOf } from 'lib-common/json'
import type { UUID } from 'lib-common/types'

const mapToAssistanceNeedVoucherCoefficient = (
  data: JsonOf<AssistanceNeedVoucherCoefficientResponse>
): AssistanceNeedVoucherCoefficientResponse => ({
  ...data,
  voucherCoefficient: {
    ...data.voucherCoefficient,
    validityPeriod: FiniteDateRange.parseJson(
      data.voucherCoefficient.validityPeriod
    )
  }
})

export function getAssistanceNeedVoucherCoefficients(
  childId: UUID
): Promise<Result<AssistanceNeedVoucherCoefficientResponse[]>> {
  return client
    .get<JsonOf<AssistanceNeedVoucherCoefficientResponse[]>>(
      `/children/${childId}/assistance-need-voucher-coefficients`
    )
    .then(({ data }) =>
      Success.of(data.map(mapToAssistanceNeedVoucherCoefficient))
    )
    .catch((e) => Failure.fromError(e))
}

export function createAssistanceNeedVoucherCoefficient(
  childId: UUID,
  data: AssistanceNeedVoucherCoefficientRequest
): Promise<Result<void>> {
  return client
    .post(`/children/${childId}/assistance-need-voucher-coefficients`, data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function updateAssistanceNeedVoucherCoefficient(
  id: UUID,
  data: AssistanceNeedVoucherCoefficientRequest
): Promise<Result<void>> {
  return client
    .put(`/assistance-need-voucher-coefficients/${id}`, data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function deleteAssistanceNeedVoucherCoefficient(
  id: UUID
): Promise<Result<void>> {
  return client
    .delete(`/assistance-need-voucher-coefficients/${id}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
