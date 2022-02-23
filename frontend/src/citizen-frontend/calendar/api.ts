// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  AbsenceRequest,
  DailyReservationRequest,
  ReservationsResponse
} from 'lib-common/generated/api-types/reservations'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

import { client } from '../api-client'

export async function getReservations(
  from: LocalDate,
  to: LocalDate
): Promise<Result<ReservationsResponse>> {
  return client
    .get<JsonOf<ReservationsResponse>>('/citizen/reservations', {
      params: { from: from.formatIso(), to: to.formatIso() }
    })
    .then((res) =>
      Success.of({
        ...res.data,
        dailyData: res.data.dailyData.map((data) => ({
          ...data,
          date: LocalDate.parseIso(data.date)
        })),
        children: res.data.children.map((child) => ({
          ...child,
          placementMinStart: LocalDate.parseIso(child.placementMinStart),
          placementMaxEnd: LocalDate.parseIso(child.placementMaxEnd)
        })),
        // TODO Array.isArray is only for backward compatibility – remove ternaries when this is in production
        reservableDays: Array.isArray(res.data.reservableDays)
          ? res.data.reservableDays.map((r) => FiniteDateRange.parseJson(r))
          : res.data.reservableDays
          ? [FiniteDateRange.parseJson(res.data.reservableDays)]
          : []
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function postReservations(
  reservations: DailyReservationRequest[]
): Promise<Result<null>> {
  return client
    .post('/citizen/reservations', reservations)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function postAbsences(
  request: AbsenceRequest
): Promise<Result<void>> {
  return client
    .post('/citizen/absences', request)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
