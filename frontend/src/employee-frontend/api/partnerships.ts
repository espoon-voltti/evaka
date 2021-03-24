// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../types'
import { Failure, Result, Success } from 'lib-common/api'
import { Partnership } from '../types/fridge'
import { client } from '../api/client'
import { deserializePersonDetails } from '../types/person'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

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
        partners: data.partners.map(deserializePersonDetails)
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
): Promise<Result<Partnership>> {
  const body = {
    personIds: [personId, partnerId],
    startDate,
    endDate
  }

  return client
    .post<JsonOf<Partnership>>('/partnerships', body)
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      startDate: LocalDate.parseIso(data.startDate),
      endDate: LocalDate.parseNullableIso(data.endDate),
      partners: data.partners.map(deserializePersonDetails)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function updatePartnership(
  id: UUID,
  startDate: LocalDate,
  endDate: LocalDate | null
): Promise<Result<Partnership>> {
  const body = {
    startDate,
    endDate
  }

  return client
    .put<JsonOf<Partnership>>(`/partnerships/${id}`, body)
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      startDate: LocalDate.parseIso(data.startDate),
      endDate: LocalDate.parseNullableIso(data.endDate),
      partners: data.partners.map(deserializePersonDetails)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function retryPartnership(id: UUID): Promise<Result<null>> {
  return client
    .put(`/partnerships/${id}/retry`)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function removePartnership(id: UUID): Promise<Result<null>> {
  return client
    .delete(`/partnerships/${id}`, {})
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}
