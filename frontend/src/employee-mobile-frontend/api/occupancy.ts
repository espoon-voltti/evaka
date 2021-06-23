// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { client } from './client'

interface OccupancyResponse {
  occupancies: OccupancyPeriod[]
  max: OccupancyPeriod | null
  min: OccupancyPeriod | null
}

export type OccupancyResponseGroupLevel = Array<{
  groupId: UUID
  occupancies: OccupancyResponse
}>

interface OccupancyPeriod {
  period: FiniteDateRange
  sum: number
  headcount: number
  caretakers: number
  percentage: number
}

export async function getRealizedOccupancyToday(
  unitId: string,
  groupId: string | undefined
): Promise<Result<number>> {
  if (groupId) {
    return (await getRealizedGroupOccupanciesToday(unitId)).map(
      (response: OccupancyResponseGroupLevel) => {
        // There's only one occupancy, because we only fetch today's occupancy, so we can use `min`
        const percentage = response.find((group) => group.groupId === groupId)
          ?.occupancies.min?.percentage
        return percentage ?? 0
      }
    )
  } else {
    return (await getRealizedUnitOccupancyToday(unitId)).map(
      (response) => response.min?.percentage ?? 0
    )
  }
}

async function getRealizedUnitOccupancyToday(
  unitId: UUID
): Promise<Result<OccupancyResponse>> {
  const today = LocalDate.today().toString()
  return await client
    .get<JsonOf<OccupancyResponse>>(`/occupancy/by-unit/${unitId}`, {
      params: { from: today, to: today, type: 'REALIZED' }
    })
    .then((res) => res.data)
    .then(mapOccupancyResponse)
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getRealizedGroupOccupanciesToday(
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
          occupancies: mapOccupancyResponse(group.occupancies)
        }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

function mapOccupancyResponse(
  response: JsonOf<OccupancyResponse>
): OccupancyResponse {
  return {
    occupancies: response.occupancies.map(mapOccupancyPeriod),
    max: mapOccupancyPeriod(response.max),
    min: mapOccupancyPeriod(response.min)
  }
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
