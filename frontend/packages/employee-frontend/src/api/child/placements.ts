// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'api/index'
import { client } from 'api/client'
import { Placement } from 'types/child'
import { UUID } from 'types'
import { PlacementType } from 'types/placementdraft'
import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'

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
    .then(() => Success(null))
    .catch(Failure)
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
    .then(Success)
    .catch(Failure)
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
    .then(() => Success(null))
    .catch(Failure)
}

export async function deletePlacement(
  placementId: UUID
): Promise<Result<null>> {
  return client
    .delete(`/placements/${placementId}`)
    .then(() => Success(null))
    .catch(Failure)
}
