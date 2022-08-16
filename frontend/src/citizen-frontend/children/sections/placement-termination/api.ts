// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'citizen-frontend/api-client'
import { Failure, Result, Success } from 'lib-common/api'
import {
  ChildPlacement,
  ChildPlacementResponse,
  PlacementTerminationRequestBody,
  TerminatablePlacementGroup
} from 'lib-common/generated/api-types/placement'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

const deserializeChildPlacement = ({
  startDate,
  endDate,
  terminationRequestedDate,
  ...rest
}: JsonOf<ChildPlacement>): ChildPlacement => ({
  ...rest,
  startDate: LocalDate.parseIso(startDate),
  endDate: LocalDate.parseIso(endDate),
  terminationRequestedDate: terminationRequestedDate
    ? LocalDate.parseIso(terminationRequestedDate)
    : null
})

const deserializeTerminatablePlacementGroup = ({
  startDate,
  endDate,
  placements,
  additionalPlacements,
  ...rest
}: JsonOf<TerminatablePlacementGroup>): TerminatablePlacementGroup => ({
  ...rest,
  startDate: LocalDate.parseIso(startDate),
  endDate: LocalDate.parseIso(endDate),
  placements: placements.map(deserializeChildPlacement),
  additionalPlacements: additionalPlacements.map(deserializeChildPlacement)
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
        placements: placements.map(deserializeTerminatablePlacementGroup)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export function terminatePlacement(
  childId: UUID,
  body: PlacementTerminationRequestBody
): Promise<Result<void>> {
  return client
    .post(`/citizen/children/${childId}/placements/terminate`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
