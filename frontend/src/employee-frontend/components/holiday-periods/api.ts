// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  HolidayPeriod,
  HolidayPeriodBody
} from 'lib-common/generated/api-types/holidaypediod'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'

const mapHolidayPeriod = ({
  created,
  updated,
  showReservationBannerFrom,
  period,
  reservationDeadline,
  freePeriod,
  ...rest
}: JsonOf<HolidayPeriod>): HolidayPeriod => ({
  ...rest,
  created: new Date(created),
  updated: new Date(updated),
  reservationDeadline: LocalDate.parseIso(reservationDeadline),
  showReservationBannerFrom: LocalDate.parseIso(showReservationBannerFrom),
  period: FiniteDateRange.parseJson(period),
  freePeriod: freePeriod
    ? {
        ...freePeriod,
        deadline: LocalDate.parseIso(freePeriod.deadline),
        periodOptions: freePeriod.periodOptions.map((opt) =>
          FiniteDateRange.parseJson(opt)
        )
      }
    : null
})

export function getHolidayPeriods(): Promise<Result<HolidayPeriod[]>> {
  return client
    .get<JsonOf<HolidayPeriod[]>>('/holiday-period')
    .then((res) => Success.of(res.data.map(mapHolidayPeriod)))
    .catch((e) => Failure.fromError(e))
}

export function getHolidayPeriod(id: UUID): Promise<Result<HolidayPeriod>> {
  return client
    .get<JsonOf<HolidayPeriod>>(`/holiday-period/${id}`)
    .then((res) => Success.of(mapHolidayPeriod(res.data)))
    .catch((e) => Failure.fromError(e))
}

export function createHolidayPeriod(
  data: HolidayPeriodBody
): Promise<Result<void>> {
  return client
    .post('/holiday-period', data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function updateHolidayPeriod(
  id: UUID,
  data: HolidayPeriodBody
): Promise<Result<void>> {
  return client
    .put(`/holiday-period/${id}`, data)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function deleteHolidayPeriod(id: UUID): Promise<Result<void>> {
  return client
    .delete(`/holiday-period/${id}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
