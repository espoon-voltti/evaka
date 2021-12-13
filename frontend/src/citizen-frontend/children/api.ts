// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  Child,
  ChildrenResponse
} from 'lib-common/generated/api-types/children'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import {
  ChildPlacement,
  ChildPlacementResponse,
  PlacementTerminationRequestBody
} from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { client } from '../api-client'

export function getChildren(): Promise<Result<ChildrenResponse>> {
  return client
    .get<JsonOf<ChildrenResponse>>('/citizen/children')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export function getChild(childId: UUID): Promise<Result<Child>> {
  return client
    .get<JsonOf<Child>>(`/citizen/children/${childId}`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

const deserializeChildPlacement = ({
  placementEndDate,
  placementStartDate,
  terminationRequestedDate,
  ...rest
}: JsonOf<ChildPlacement>): ChildPlacement => ({
  ...rest,
  placementStartDate: LocalDate.parseIso(placementStartDate),
  placementEndDate: LocalDate.parseIso(placementEndDate),
  terminationRequestedDate: terminationRequestedDate
    ? LocalDate.parseIso(terminationRequestedDate)
    : null
})

export function getPlacements(
  childId: UUID
): Promise<Result<ChildPlacementResponse>> {
  return client
    .get<JsonOf<ChildPlacementResponse>>(
      `/citizen/children/${childId}/placements`
    )
    .then(({ data: { placements, ...rest } }) =>
      Success.of({
        ...rest,
        placements: placements.map(deserializeChildPlacement)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export function terminatePlacement(
  body: PlacementTerminationRequestBody
): Promise<Result<void>> {
  return client
    .post(`/citizen/placements/terminate`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
