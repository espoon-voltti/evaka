// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ConfirmedRangeDate } from 'lib-common/generated/api-types/reservations'
import type { ConfirmedRangeDateUpdate } from 'lib-common/generated/api-types/reservations'
import type { DailyChildReservationResult } from 'lib-common/generated/api-types/reservations'
import type { DayReservationStatisticsResult } from 'lib-common/generated/api-types/reservations'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import { client } from '../../client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonConfirmedRangeDate } from 'lib-common/generated/api-types/reservations'
import { deserializeJsonDailyChildReservationResult } from 'lib-common/generated/api-types/reservations'
import { deserializeJsonDayReservationStatisticsResult } from 'lib-common/generated/api-types/reservations'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.getChildReservationsForDay
*/
export async function getChildReservationsForDay(
  request: {
    unitId: DaycareId,
    examinationDate: LocalDate
  }
): Promise<DailyChildReservationResult> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['examinationDate', request.examinationDate.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<DailyChildReservationResult>>({
    url: uri`/employee-mobile/attendance-reservations/confirmed-days/daily`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonDailyChildReservationResult(json)
}


/**
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.getConfirmedRangeData
*/
export async function getConfirmedRangeData(
  request: {
    childId: PersonId
  }
): Promise<ConfirmedRangeDate[]> {
  const { data: json } = await client.request<JsonOf<ConfirmedRangeDate[]>>({
    url: uri`/employee-mobile/attendance-reservations/by-child/${request.childId}/confirmed-range`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonConfirmedRangeDate(e))
}


/**
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.getReservationStatisticsForConfirmedDays
*/
export async function getReservationStatisticsForConfirmedDays(
  request: {
    unitId: DaycareId
  }
): Promise<DayReservationStatisticsResult[]> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId]
  )
  const { data: json } = await client.request<JsonOf<DayReservationStatisticsResult[]>>({
    url: uri`/employee-mobile/attendance-reservations/confirmed-days/stats`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonDayReservationStatisticsResult(e))
}


/**
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.setConfirmedRangeReservations
*/
export async function setConfirmedRangeReservations(
  request: {
    childId: PersonId,
    body: ConfirmedRangeDateUpdate[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendance-reservations/by-child/${request.childId}/confirmed-range`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ConfirmedRangeDateUpdate[]>
  })
  return json
}
