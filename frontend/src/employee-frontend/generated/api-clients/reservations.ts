// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { ChildDatePresence } from 'lib-common/generated/api-types/reservations'
import { DailyReservationRequest } from 'lib-common/generated/api-types/reservations'
import { ExpectedAbsencesRequest } from 'lib-common/generated/api-types/reservations'
import { ExpectedAbsencesResponse } from 'lib-common/generated/api-types/reservations'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { UnitAttendanceReservations } from 'lib-common/generated/api-types/reservations'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonUnitAttendanceReservations } from 'lib-common/generated/api-types/reservations'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.getAttendanceReservations
*/
export async function getAttendanceReservations(
  request: {
    unitId: UUID,
    from: LocalDate,
    to: LocalDate,
    includeNonOperationalDays?: boolean | null
  }
): Promise<UnitAttendanceReservations> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()],
    ['includeNonOperationalDays', request.includeNonOperationalDays?.toString()]
  )
  const { data: json } = await client.request<JsonOf<UnitAttendanceReservations>>({
    url: uri`/employee/attendance-reservations`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonUnitAttendanceReservations(json)
}


/**
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.getExpectedAbsences
*/
export async function getExpectedAbsences(
  request: {
    body: ExpectedAbsencesRequest
  }
): Promise<ExpectedAbsencesResponse> {
  const { data: json } = await client.request<JsonOf<ExpectedAbsencesResponse>>({
    url: uri`/employee/attendance-reservations/child-date/expected-absences`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ExpectedAbsencesRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.postChildDatePresence
*/
export async function postChildDatePresence(
  request: {
    body: ChildDatePresence
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/attendance-reservations/child-date`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ChildDatePresence>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.postReservations
*/
export async function postReservations(
  request: {
    body: DailyReservationRequest[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/attendance-reservations`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DailyReservationRequest[]>
  })
  return json
}
