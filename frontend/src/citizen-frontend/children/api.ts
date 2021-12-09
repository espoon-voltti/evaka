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
import { ChildPlacement } from 'lib-common/generated/api-types/placement'
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

export function getPlacements(
  childId: UUID
): Promise<Result<ChildPlacement[]>> {
  return client
    .get<JsonOf<ChildPlacement[]>>(`/citizen/children/${childId}/placements`)
    .then((res) =>
      Success.of(
        res.data.map(
          ({
            placementEndDate,
            placementStartDate,
            terminationRequestedDate,
            ...rest
          }) => ({
            ...rest,
            placementStartDate: LocalDate.parseIso(placementStartDate),
            placementEndDate: LocalDate.parseIso(placementEndDate),
            terminationRequestedDate: terminationRequestedDate
              ? LocalDate.parseIso(terminationRequestedDate)
              : null
          })
        )
      )
    )
    .catch((e) => Failure.fromError(e))
}
