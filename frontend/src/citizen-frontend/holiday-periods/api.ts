// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { deserializeHolidayPeriod } from 'lib-common/api-types/holiday-period'
import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { JsonOf } from 'lib-common/json'
import { client } from '../api-client'

export function getActionRequiringHolidayPeriods(): Promise<
  Result<HolidayPeriod[]>
> {
  return client
    .get<JsonOf<HolidayPeriod[]>>(`/citizen/holiday-period/action-required`)
    .then((res) => Success.of(res.data.map(deserializeHolidayPeriod)))
    .catch((e) => Failure.fromError(e))
}
