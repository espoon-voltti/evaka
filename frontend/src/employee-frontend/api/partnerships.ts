// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { Partnership } from 'lib-common/generated/api-types/pis'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { deserializePersonJSON } from '../types/person'
import { client } from './client'

export async function getPartnerships(
  id: UUID
): Promise<Result<Partnership[]>> {
  return client
    .get<JsonOf<Partnership[]>>('/partnerships', {
      params: { personId: id }
    })
    .then((res) => res.data)
    .then((dataArray) =>
      dataArray.map((data) => ({
        ...data,
        startDate: LocalDate.parseIso(data.startDate),
        endDate: LocalDate.parseNullableIso(data.endDate),
        partners: data.partners.map(deserializePersonJSON)
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function addPartnership(
  personId: UUID,
  partnerId: UUID,
  startDate: LocalDate,
  endDate: LocalDate | null
): Promise<Result<void>> {
  const body = {
    person1Id: personId,
    person2Id: partnerId,
    startDate,
    endDate
  }

  return client
    .post<JsonOf<Partnership>>('/partnerships', body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function updatePartnership(
  id: UUID,
  startDate: LocalDate,
  endDate: LocalDate | null
): Promise<Result<void>> {
  const body = {
    startDate,
    endDate
  }

  return client
    .put(`/partnerships/${id}`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function retryPartnership(id: UUID): Promise<Result<void>> {
  return client
    .put(`/partnerships/${id}/retry`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function removePartnership(id: UUID): Promise<Result<void>> {
  return client
    .delete(`/partnerships/${id}`, {})
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
