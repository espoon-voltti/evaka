// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from '@evaka/lib-common/api'
import { client } from '../../api/client'
import { Placement, PlacementType } from '../../types/child'
import { UUID } from '../../types'
import { JsonOf } from '@evaka/lib-common/json'
import LocalDate from '@evaka/lib-common/local-date'

export interface PlacementCreate {
  childId: UUID
  type: PlacementType
  unitId: UUID
  startDate: LocalDate
  endDate: LocalDate
}

export interface PlacementUpdate {
  startDate: LocalDate
  endDate: LocalDate
}

export async function createPlacement(
  body: PlacementCreate
): Promise<Result<null>> {
  return client
    .post<JsonOf<Placement>>(`/placements`, {
      ...body,
      startDate: body.startDate,
      endDate: body.endDate
    })
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function getPlacements(
  childId: UUID
): Promise<Result<Placement[]>> {
  const config = {
    params: {
      childId
    }
  }
  return client
    .get<JsonOf<Placement[]>>('/placements', config)
    .then((res) => res.data)
    .then((data) =>
      data.map((p) => ({
        ...p,
        startDate: LocalDate.parseIso(p.startDate),
        endDate: LocalDate.parseIso(p.endDate)
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function updatePlacement(
  placementId: UUID,
  body: PlacementUpdate
): Promise<Result<null>> {
  return client
    .put(`/placements/${placementId}`, {
      ...body,
      startDate: body.startDate,
      endDate: body.endDate
    })
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function deletePlacement(
  placementId: UUID
): Promise<Result<null>> {
  return client
    .delete(`/placements/${placementId}`)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}
