// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Absence } from 'lib-common/generated/api-types/absence'
import type { AbsenceApplicationId } from 'lib-common/generated/api-types/shared'
import type { AbsenceApplicationRejectRequest } from 'lib-common/generated/api-types/absence'
import type { AbsenceApplicationStatus } from 'lib-common/generated/api-types/absence'
import type { AbsenceApplicationSummaryEmployee } from 'lib-common/generated/api-types/absence'
import type { AbsenceUpsert } from 'lib-common/generated/api-types/absence'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type { GroupId } from 'lib-common/generated/api-types/shared'
import type { GroupMonthCalendar } from 'lib-common/generated/api-types/absence'
import type { HolidayReservationsDelete } from 'lib-common/generated/api-types/absence'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { Presence } from 'lib-common/generated/api-types/absence'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonAbsence } from 'lib-common/generated/api-types/absence'
import { deserializeJsonAbsenceApplicationSummaryEmployee } from 'lib-common/generated/api-types/absence'
import { deserializeJsonGroupMonthCalendar } from 'lib-common/generated/api-types/absence'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.absence.AbsenceController.addPresences
*/
export async function addPresences(
  request: {
    groupId: GroupId,
    body: Presence[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/absences/${request.groupId}/present`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<Presence[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.absence.AbsenceController.deleteHolidayReservations
*/
export async function deleteHolidayReservations(
  request: {
    groupId: GroupId,
    body: HolidayReservationsDelete[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/absences/${request.groupId}/delete-holiday-reservations`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<HolidayReservationsDelete[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.absence.AbsenceController.getAbsencesOfChild
*/
export async function getAbsencesOfChild(
  request: {
    childId: PersonId,
    year: number,
    month: number
  }
): Promise<Absence[]> {
  const params = createUrlSearchParams(
    ['year', request.year.toString()],
    ['month', request.month.toString()]
  )
  const { data: json } = await client.request<JsonOf<Absence[]>>({
    url: uri`/employee/absences/by-child/${request.childId}`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonAbsence(e))
}


/**
* Generated from fi.espoo.evaka.absence.AbsenceController.groupMonthCalendar
*/
export async function groupMonthCalendar(
  request: {
    groupId: GroupId,
    year: number,
    month: number
  }
): Promise<GroupMonthCalendar> {
  const params = createUrlSearchParams(
    ['year', request.year.toString()],
    ['month', request.month.toString()]
  )
  const { data: json } = await client.request<JsonOf<GroupMonthCalendar>>({
    url: uri`/employee/absences/${request.groupId}`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonGroupMonthCalendar(json)
}


/**
* Generated from fi.espoo.evaka.absence.AbsenceController.upsertAbsences
*/
export async function upsertAbsences(
  request: {
    groupId: GroupId,
    body: AbsenceUpsert[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/absences/${request.groupId}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AbsenceUpsert[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.absence.application.AbsenceApplicationControllerEmployee.acceptAbsenceApplication
*/
export async function acceptAbsenceApplication(
  request: {
    id: AbsenceApplicationId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/absence-application/${request.id}/accept`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.absence.application.AbsenceApplicationControllerEmployee.getAbsenceApplications
*/
export async function getAbsenceApplications(
  request: {
    unitId?: DaycareId | null,
    childId?: PersonId | null,
    status?: AbsenceApplicationStatus | null
  }
): Promise<AbsenceApplicationSummaryEmployee[]> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['childId', request.childId],
    ['status', request.status?.toString()]
  )
  const { data: json } = await client.request<JsonOf<AbsenceApplicationSummaryEmployee[]>>({
    url: uri`/employee/absence-application`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonAbsenceApplicationSummaryEmployee(e))
}


/**
* Generated from fi.espoo.evaka.absence.application.AbsenceApplicationControllerEmployee.rejectAbsenceApplication
*/
export async function rejectAbsenceApplication(
  request: {
    id: AbsenceApplicationId,
    body: AbsenceApplicationRejectRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/absence-application/${request.id}/reject`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AbsenceApplicationRejectRequest>
  })
  return json
}
