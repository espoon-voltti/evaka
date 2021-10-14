// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import FiniteDateRange from 'lib-common/finite-date-range'
import { Failure, Result, Success } from 'lib-common/api'
import { client } from '../api-client'
import { JsonOf } from 'lib-common/json'
import { AbsenceType } from 'lib-common/generated/enums'
import {
  DailyReservationRequest,
  ReservationsResponse
} from 'lib-common/generated/api-types/reservations'

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
        reservableDays: FiniteDateRange.parseJson(res.data.reservableDays)
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

export interface AbsencesRequest {
  childIds: string[]
  dateRange: FiniteDateRange
  absenceType: AbsenceType
}

export async function postAbsences(
  request: AbsencesRequest
): Promise<Result<void>> {
  return client
    .post('/citizen/absences', request)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
