// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { Parentship } from 'lib-common/generated/api-types/pis'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { deserializePersonJSON } from '../types/person'
import { client } from './client'

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
        endDate: LocalDate.parseIso(data.endDate),
        headOfChild: deserializePersonJSON(data.headOfChild),
        child: deserializePersonJSON(data.child)
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
  endDate: LocalDate
): Promise<Result<void>> {
  const body = {
    headOfChildId,
    childId,
    startDate,
    endDate
  }
  return client
    .post('/parentships', body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function updateParentship(
  id: UUID,
  startDate: LocalDate,
  endDate: LocalDate
): Promise<Result<void>> {
  const body = {
    startDate,
    endDate
  }
  return client
    .put(`/parentships/${id}`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function retryParentship(id: UUID): Promise<Result<void>> {
  return client
    .put(`/parentships/${id}/retry`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function removeParentship(id: UUID): Promise<Result<void>> {
  return client
    .delete(`/parentships/${id}`, {})
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
