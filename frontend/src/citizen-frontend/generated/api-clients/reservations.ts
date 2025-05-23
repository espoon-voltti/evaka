// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { AbsenceRequest } from 'lib-common/generated/api-types/reservations'
import type { DailyReservationRequest } from 'lib-common/generated/api-types/reservations'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { OperationalDatesRequest } from 'lib-common/generated/api-types/reservations'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { ReservationsResponse } from 'lib-common/generated/api-types/reservations'
import { client } from '../../api-client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonReservationsResponse } from 'lib-common/generated/api-types/reservations'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.reservations.ReservationControllerCitizen.getPreschoolOperationalDates
*/
export async function getPreschoolOperationalDates(
  request: {
    body: OperationalDatesRequest
  }
): Promise<Partial<Record<PersonId, LocalDate[]>>> {
  const { data: json } = await client.request<JsonOf<Partial<Record<PersonId, LocalDate[]>>>>({
    url: uri`/citizen/preschool-operational-dates`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<OperationalDatesRequest>
  })
  return Object.fromEntries(Object.entries(json).map(
    ([k, v]) => [k, v !== undefined ? v.map(e => LocalDate.parseIso(e)) : v]
  ))
}


/**
* Generated from fi.espoo.evaka.reservations.ReservationControllerCitizen.getReservations
*/
export async function getReservations(
  request: {
    from: LocalDate,
    to: LocalDate
  }
): Promise<ReservationsResponse> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<ReservationsResponse>>({
    url: uri`/citizen/reservations`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonReservationsResponse(json)
}


/**
* Generated from fi.espoo.evaka.reservations.ReservationControllerCitizen.postAbsences
*/
export async function postAbsences(
  request: {
    body: AbsenceRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/absences`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AbsenceRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reservations.ReservationControllerCitizen.postReservations
*/
export async function postReservations(
  request: {
    body: DailyReservationRequest[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/reservations`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DailyReservationRequest[]>
  })
  return json
}
