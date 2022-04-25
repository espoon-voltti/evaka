// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { DailyServiceTimes } from 'lib-common/api-types/child/common'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

import { client } from '../client'

interface DailyServiceTimesResponse {
  dailyServiceTimes: DailyServiceTimes | null
}

export async function getChildDailyServiceTimes(
  childId: UUID
): Promise<Result<DailyServiceTimes | null>> {
  return client
    .get<JsonOf<DailyServiceTimesResponse>>(
      `/children/${childId}/daily-service-times`
    )
    .then((res) => Success.of(res.data.dailyServiceTimes))
    .catch((e) => Failure.fromError(e))
}

export async function putChildDailyServiceTimes(
  childId: UUID,
  data: DailyServiceTimes
): Promise<Result<void>> {
  return client
    .put(`/children/${childId}/daily-service-times`, data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function deleteChildDailyServiceTimes(
  childId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/children/${childId}/daily-service-times`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
