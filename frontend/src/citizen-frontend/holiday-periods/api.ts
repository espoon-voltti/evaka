// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { deserializeHolidayPeriod } from 'lib-common/api-types/holiday-period'
import {
  HolidayPeriod,
  HolidayAbsenceRequest
} from 'lib-common/generated/api-types/holidayperiod'
import { JsonOf } from 'lib-common/json'

import { client } from '../api-client'

export function getHolidayPeriods(): Promise<Result<HolidayPeriod[]>> {
  return client
    .get<JsonOf<HolidayPeriod[]>>(`/citizen/holiday-period`)
    .then((res) => Success.of(res.data.map(deserializeHolidayPeriod)))
    .catch((e) => Failure.fromError(e))
}

export async function postHolidayAbsences(
  request: HolidayAbsenceRequest
): Promise<Result<void>> {
  return client
    .post('/citizen/holiday-period/holidays', request)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
