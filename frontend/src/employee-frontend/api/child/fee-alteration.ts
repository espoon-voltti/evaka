// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  FeeAlteration,
  FeeAlterationWithPermittedActions
} from 'lib-common/generated/api-types/invoicing'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { PartialFeeAlteration } from '../../types/fee-alteration'
import { client } from '../client'

export async function getFeeAlterations(
  personId: UUID
): Promise<Result<FeeAlterationWithPermittedActions[]>> {
  return client
    .get<JsonOf<FeeAlterationWithPermittedActions[]>>(
      `/fee-alterations?personId=${personId}`
    )
    .then((res) => res.data)
    .then((data) =>
      data.map(({ data: feeAlteration, permittedActions }) => ({
        data: {
          ...feeAlteration,
          validFrom: LocalDate.parseIso(feeAlteration.validFrom),
          validTo: LocalDate.parseNullableIso(feeAlteration.validTo),
          updatedAt:
            feeAlteration.updatedAt !== null
              ? HelsinkiDateTime.parseIso(feeAlteration.updatedAt)
              : null
        },
        permittedActions
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function createFeeAlteration(
  feeAlteration: PartialFeeAlteration
): Promise<Result<void>> {
  return client
    .post<void>('/fee-alterations', feeAlteration)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function updateFeeAlteration(
  feeAlteration: FeeAlteration
): Promise<Result<void>> {
  if (feeAlteration.id === null) {
    throw Error('Fee alteration id is null')
  }
  return client
    .put<void>(`/fee-alterations/${feeAlteration.id}`, feeAlteration)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function deleteFeeAlteration(id: UUID): Promise<Result<void>> {
  return client
    .delete<void>(`/fee-alterations/${id}`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
