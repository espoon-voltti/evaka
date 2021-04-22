// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import { DailyServiceTimes } from 'lib-common/api-types/child/common'
import { UUID } from '../../types'
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
): Promise<Result<null>> {
  return client
    .put(`/children/${childId}/daily-service-times`, data)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function deleteChildDailyServiceTimes(
  childId: UUID
): Promise<Result<null>> {
  return client
    .delete(`/children/${childId}/daily-service-times`)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}
