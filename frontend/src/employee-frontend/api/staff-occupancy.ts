// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import type {
  StaffOccupancyCoefficient,
  OccupancyCoefficientUpsert
} from 'lib-common/generated/api-types/attendance'
import type { JsonOf } from 'lib-common/json'
import type { UUID } from 'lib-common/types'

import { client } from './client'

export function getOccupancyCoefficients(
  unitId: UUID
): Promise<Result<StaffOccupancyCoefficient[]>> {
  return client
    .get<JsonOf<StaffOccupancyCoefficient[]>>('/occupancy-coefficient', {
      params: { unitId }
    })
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export function upsertOccupancyCoefficient(
  body: OccupancyCoefficientUpsert
): Promise<Result<void>> {
  return client
    .post('/occupancy-coefficient', body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
