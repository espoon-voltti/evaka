// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { client } from './client'

export type OccupancyResponseGroupLevel = Array<{
  groupId: UUID
  occupancies: OccupancyResponse
}>

interface OccupancyResponse {
  occupancies: OccupancyPeriod[]
  max: OccupancyPeriod | null
  min: OccupancyPeriod | null
}

interface OccupancyPeriod {
  period: FiniteDateRange
  sum: number
  headcount: number
  caretakers: number
  percentage: number
}

export async function getRealizedOccupanciesToday(
  unitId: UUID
): Promise<Result<OccupancyResponseGroupLevel>> {
  const today = LocalDate.today().toString()
  return await client
    .get<JsonOf<OccupancyResponseGroupLevel>>(
      `/occupancy/by-unit/${unitId}/groups`,
      { params: { from: today, to: today, type: 'REALIZED' } }
    )
    .then((res) => res.data)
    .then(
      (data): OccupancyResponseGroupLevel =>
        data.map((group) => ({
          ...group,
          occupancies: {
            occupancies: group.occupancies.occupancies.map(mapOccupancyPeriod),
            max: mapOccupancyPeriod(group.occupancies.max),
            min: mapOccupancyPeriod(group.occupancies.min)
          }
        }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

function mapOccupancyPeriod(
  period: JsonOf<OccupancyPeriod> | null
): OccupancyPeriod | null
function mapOccupancyPeriod(period: JsonOf<OccupancyPeriod>): OccupancyPeriod
function mapOccupancyPeriod(
  period: JsonOf<OccupancyPeriod> | null
): OccupancyPeriod | null {
  if (!period) return null
  return {
    ...period,
    period: FiniteDateRange.parseJson(period.period)
  }
}
