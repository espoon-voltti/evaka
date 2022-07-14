// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { DailyServiceTimes } from 'lib-common/api-types/child/common'
import DateRange from 'lib-common/date-range'
import { DailyServiceTimesResponse } from 'lib-common/generated/api-types/dailyservicetimes'
import { JsonOf } from 'lib-common/json'
import { OmitInUnion, UUID } from 'lib-common/types'

import { client } from '../client'

export async function getChildDailyServiceTimes(
  childId: UUID
): Promise<Result<DailyServiceTimesResponse[]>> {
  return client
    .get<JsonOf<DailyServiceTimesResponse[]>>(
      `/children/${childId}/daily-service-times`
    )
    .then((res) =>
      Success.of(
        res.data.map((response) => ({
          ...response,
          dailyServiceTimes: {
            ...response.dailyServiceTimes,
            times: {
              ...response.dailyServiceTimes.times,
              validityPeriod: DateRange.parseJson(
                response.dailyServiceTimes.times.validityPeriod
              )
            }
          }
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export type DailyServiceTimesRequestData = OmitInUnion<DailyServiceTimes, 'id'>

export async function createChildDailyServiceTimes(
  childId: UUID,
  data: DailyServiceTimesRequestData
): Promise<Result<void>> {
  return client
    .post(`/children/${childId}/daily-service-times`, data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function putChildDailyServiceTimes(
  id: UUID,
  data: DailyServiceTimesRequestData
): Promise<Result<void>> {
  return client
    .put(`/daily-service-times/${id}`, data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function deleteChildDailyServiceTimes(
  id: UUID
): Promise<Result<void>> {
  return client
    .delete(`/daily-service-times/${id}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
