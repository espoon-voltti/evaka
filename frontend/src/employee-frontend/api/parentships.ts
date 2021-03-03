// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../types'
import { Failure, Result, Success } from '@evaka/lib-common/src/api'
import { Parentship } from '../types/fridge'
import { client } from '../api/client'
import { deserializePersonDetails } from '../types/person'
import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'

async function getParentships(
  headOfChildId?: UUID,
  childId?: UUID
): Promise<Result<Parentship[]>> {
  return client
    .get<JsonOf<Parentship[]>>('/parentships', {
      params: { headOfChildId, childId }
    })
    .then((res) => res.data)
    .then((dataArray) =>
      dataArray.map((data) => ({
        ...data,
        startDate: LocalDate.parseIso(data.startDate),
        endDate: LocalDate.parseNullableIso(data.endDate),
        headOfChild: deserializePersonDetails(data.headOfChild),
        child: deserializePersonDetails(data.child)
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getParentshipsByHeadOfChild(
  id: UUID
): Promise<Result<Parentship[]>> {
  return getParentships(id, undefined)
}

export async function getParentshipsByChild(
  id: UUID
): Promise<Result<Parentship[]>> {
  return getParentships(undefined, id)
}

export async function addParentship(
  headOfChildId: UUID,
  childId: UUID,
  startDate: LocalDate,
  endDate: LocalDate | null
): Promise<Result<Parentship>> {
  const body = {
    headOfChildId,
    childId,
    startDate,
    endDate
  }
  return client
    .post<JsonOf<Parentship>>('/parentships', body)
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      startDate: LocalDate.parseIso(data.startDate),
      endDate: LocalDate.parseNullableIso(data.endDate),
      headOfChild: deserializePersonDetails(data.headOfChild),
      child: deserializePersonDetails(data.child)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function updateParentship(
  id: UUID,
  startDate: LocalDate,
  endDate: LocalDate | null
): Promise<Result<Parentship>> {
  const body = {
    startDate,
    endDate
  }
  return client
    .put<JsonOf<Parentship>>(`/parentships/${id}`, body)
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      startDate: LocalDate.parseIso(data.startDate),
      endDate: LocalDate.parseNullableIso(data.endDate),
      headOfChild: deserializePersonDetails(data.headOfChild),
      child: deserializePersonDetails(data.child)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function retryParentship(id: UUID): Promise<Result<null>> {
  return client
    .put(`/parentships/${id}/retry`)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function removeParentship(id: UUID): Promise<Result<null>> {
  return client
    .delete(`/parentships/${id}`, {})
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}
