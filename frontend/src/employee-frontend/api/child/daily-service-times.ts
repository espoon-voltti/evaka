// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, type Result, Success } from 'lib-common/api'
import { parseDailyServiceTimes } from 'lib-common/api-types/daily-service-times'
import {
  type DailyServiceTimesEndDate,
  type DailyServiceTimesResponse,
  type DailyServiceTimesValue
} from 'lib-common/generated/api-types/dailyservicetimes'
import { type JsonOf } from 'lib-common/json'
import { type UUID } from 'lib-common/types'

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
            times: parseDailyServiceTimes(response.dailyServiceTimes.times)
          }
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function createChildDailyServiceTimes(
  childId: UUID,
  data: DailyServiceTimesValue
): Promise<Result<void>> {
  return client
    .post(`/children/${childId}/daily-service-times`, data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function updateChildDailyServiceTimes(
  id: UUID,
  data: DailyServiceTimesValue
): Promise<Result<void>> {
  return client
    .put(`/daily-service-times/${id}`, data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function setChildDailyServiceTimesEndDate(
  id: UUID,
  data: DailyServiceTimesEndDate
): Promise<Result<void>> {
  return client
    .put(`/daily-service-times/${id}/end`, data)
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
