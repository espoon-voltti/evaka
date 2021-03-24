// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../types'
import { CaretakersResponse } from '../types/caretakers'
import { Failure, Result, Success } from 'lib-common/api'
import { client } from '../api/client'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

export async function getCaretakers(
  unitId: UUID,
  groupId: UUID
): Promise<Result<CaretakersResponse>> {
  return client
    .get<JsonOf<CaretakersResponse>>(
      `/daycares/${unitId}/groups/${groupId}/caretakers`
    )
    .then((res) => ({
      ...res.data,
      caretakers: res.data.caretakers.map((c) => ({
        ...c,
        startDate: LocalDate.parseIso(c.startDate),
        endDate: LocalDate.parseNullableIso(c.endDate)
      }))
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function postCaretakers(
  unitId: UUID,
  groupId: UUID,
  startDate: LocalDate,
  endDate: LocalDate | null,
  amount: number
): Promise<Result<null>> {
  return client
    .post(`/daycares/${unitId}/groups/${groupId}/caretakers`, {
      startDate: startDate,
      endDate: endDate,
      amount
    })
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function putCaretakers(
  unitId: UUID,
  groupId: UUID,
  id: UUID,
  startDate: LocalDate,
  endDate: LocalDate | null,
  amount: number
): Promise<Result<null>> {
  return client
    .put(`/daycares/${unitId}/groups/${groupId}/caretakers/${id}`, {
      startDate: startDate,
      endDate: endDate,
      amount
    })
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function deleteCaretakers(
  unitId: UUID,
  groupId: UUID,
  id: UUID
): Promise<Result<null>> {
  return client
    .delete(`/daycares/${unitId}/groups/${groupId}/caretakers/${id}`)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}
