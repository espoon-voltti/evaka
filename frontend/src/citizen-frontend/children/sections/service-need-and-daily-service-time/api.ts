// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'citizen-frontend/api-client'
import { Failure, Result, Success } from 'lib-common/api'
import { parseDailyServiceTimes } from 'lib-common/api-types/daily-service-times'
import { AttendanceSummary } from 'lib-common/generated/api-types/children'
import { DailyServiceTimes } from 'lib-common/generated/api-types/dailyservicetimes'
import { ServiceNeedSummary } from 'lib-common/generated/api-types/serviceneed'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

export function getServiceNeeds(
  childId: UUID
): Promise<Result<ServiceNeedSummary[]>> {
  return client
    .get<JsonOf<ServiceNeedSummary[]>>(
      `/citizen/children/${childId}/service-needs`
    )
    .then(({ data }) =>
      Success.of(
        data.map((row) => ({
          ...row,
          startDate: LocalDate.parseIso(row.startDate),
          endDate: LocalDate.parseIso(row.endDate)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export function getAttendanceSummary(
  childId: UUID,
  date: LocalDate
): Promise<Result<AttendanceSummary>> {
  return client
    .get<JsonOf<AttendanceSummary>>(
      `/citizen/children/${childId}/attendance-summary/${date.formatExotic(
        'yyyy-MM'
      )}`
    )
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export function getDailyServiceTimes(
  childId: UUID
): Promise<Result<DailyServiceTimes[]>> {
  return client
    .get<JsonOf<DailyServiceTimes[]>>(
      `/citizen/children/${childId}/daily-service-times`
    )
    .then(({ data }) =>
      Success.of(
        data.map((row) => ({
          ...row,
          times: parseDailyServiceTimes(row.times)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}
