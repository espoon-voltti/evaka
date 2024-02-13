// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import {
  AbsenceCategory,
  AbsenceType,
  GroupMonthCalendarChild,
  GroupMonthCalendarDay,
  GroupMonthCalendarDayChild
} from 'lib-common/generated/api-types/absence'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { parseReservation } from 'lib-common/reservations'
import TimeRange from 'lib-common/time-range'
import { UUID } from 'lib-common/types'

export const defaultAbsenceType = 'SICKLEAVE'
export const defaultAbsenceCategories: AbsenceCategory[] = []

export const absenceCategories: AbsenceCategory[] = ['NONBILLABLE', 'BILLABLE']

type AbsenceTypeWithBackupCare = AbsenceType | 'TEMPORARY_RELOCATION'

export interface CellPart {
  childId: UUID
  date: LocalDate
  absenceType: AbsenceTypeWithBackupCare | undefined
  category: AbsenceCategory
  position: 'left' | 'right'
}

export const deserializeGroupMonthCalendarChild = (
  json: JsonOf<GroupMonthCalendarChild>
): GroupMonthCalendarChild => ({
  ...json,
  dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
  actualServiceNeeds: json.actualServiceNeeds.map((serviceNeed) => ({
    ...serviceNeed,
    validDuring: FiniteDateRange.parseJson(serviceNeed.validDuring)
  }))
})

export const deserializeGroupMonthCalendarDay = (
  json: JsonOf<GroupMonthCalendarDay>
): GroupMonthCalendarDay => ({
  ...json,
  date: LocalDate.parseIso(json.date),
  children: json.children
    ? json.children.map(deserializeGroupMonthCalendarDayChild)
    : null
})

const deserializeGroupMonthCalendarDayChild = (
  json: JsonOf<GroupMonthCalendarDayChild>
): GroupMonthCalendarDayChild => ({
  ...json,
  dailyServiceTimes:
    json.dailyServiceTimes !== null
      ? TimeRange.parseJson(json.dailyServiceTimes)
      : null,
  absences: json.absences.map((absence) => ({
    ...absence,
    modifiedAt: HelsinkiDateTime.parseIso(absence.modifiedAt)
  })),
  reservations: json.reservations.map((reservation) => ({
    ...reservation,
    created: HelsinkiDateTime.parseIso(reservation.created),
    reservation: parseReservation(reservation.reservation)
  }))
})
