// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import type {
  CreateFosterParentRelationshipBody,
  FosterParentRelationship
} from 'lib-common/generated/api-types/pis'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import { client } from './client'

export async function getFosterChildren(
  id: UUID
): Promise<Result<FosterParentRelationship[]>> {
  return client
    .get<JsonOf<FosterParentRelationship[]>>(`/foster-parent/by-parent/${id}`)
    .then((res) =>
      Success.of(res.data.map(deserializeFosterParentRelationship))
    )
    .catch((e) => Failure.fromError(e))
}

export async function getFosterParents(
  id: UUID
): Promise<Result<FosterParentRelationship[]>> {
  return client
    .get<JsonOf<FosterParentRelationship[]>>(`/foster-parent/by-child/${id}`)
    .then((res) =>
      Success.of(res.data.map(deserializeFosterParentRelationship))
    )
    .catch((e) => Failure.fromError(e))
}

const deserializeFosterParentRelationship = (
  json: JsonOf<FosterParentRelationship>
): FosterParentRelationship => ({
  ...json,
  child: {
    ...json.child,
    dateOfBirth: LocalDate.parseIso(json.child.dateOfBirth),
    dateOfDeath: LocalDate.parseNullableIso(json.child.dateOfDeath)
  },
  parent: {
    ...json.parent,
    dateOfBirth: LocalDate.parseIso(json.parent.dateOfBirth),
    dateOfDeath: LocalDate.parseNullableIso(json.parent.dateOfDeath)
  },
  validDuring: DateRange.parseJson(json.validDuring)
})

export async function createFosterChildRelationship(
  body: CreateFosterParentRelationshipBody
): Promise<Result<void>> {
  return client
    .post<void>('/foster-parent', body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function deleteFosterChildRelationship(
  id: UUID
): Promise<Result<void>> {
  return client
    .delete<void>(`/foster-parent/${id}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function updateFosterChildRelationship(
  id: UUID,
  validDuring: DateRange
): Promise<Result<void>> {
  return client
    .post<void>(`/foster-parent/${id}`, validDuring)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
