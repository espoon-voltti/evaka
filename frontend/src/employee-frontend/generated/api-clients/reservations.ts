// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ChildDatePresence } from 'lib-common/generated/api-types/reservations'
import type { DailyReservationRequest } from 'lib-common/generated/api-types/reservations'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type { ExpectedAbsencesRequest } from 'lib-common/generated/api-types/reservations'
import type { ExpectedAbsencesResponse } from 'lib-common/generated/api-types/reservations'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { OngoingAttendanceResponse } from 'lib-common/generated/api-types/reservations'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { UnitAttendanceReservations } from 'lib-common/generated/api-types/reservations'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonOngoingAttendanceResponse } from 'lib-common/generated/api-types/reservations'
import { deserializeJsonUnitAttendanceReservations } from 'lib-common/generated/api-types/reservations'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.getAttendanceReservations
*/
export async function getAttendanceReservations(
  request: {
    unitId: DaycareId,
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
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.getOngoingChildAttendance
*/
export async function getOngoingChildAttendance(
  request: {
    childId: PersonId
  }
): Promise<OngoingAttendanceResponse> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<OngoingAttendanceResponse>>({
    url: uri`/employee/attendance-reservations/ongoing`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonOngoingAttendanceResponse(json)
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
