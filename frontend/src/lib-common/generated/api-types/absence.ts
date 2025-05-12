// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import TimeRange from '../../time-range'
import { AbsenceApplicationId } from './shared'
import { Action } from '../action'
import { EvakaUser } from './user'
import { EvakaUserType } from './user'
import { GroupId } from './shared'
import { JsonOf } from '../../json'
import { PersonId } from './shared'
import { PersonNameDetails } from './pis'
import { Reservation } from './reservations'
import { ScheduleType } from './placement'
import { ServiceTimesPresenceStatus } from './dailyservicetimes'
import { ShiftCareType } from './serviceneed'
import { deserializeJsonReservation } from './reservations'
import { deserializeJsonServiceTimesPresenceStatus } from './dailyservicetimes'

/**
* Generated from fi.espoo.evaka.absence.Absence
*/
export interface Absence {
  absenceType: AbsenceType
  belongsToQuestionnaire: boolean
  category: AbsenceCategory
  childId: PersonId
  date: LocalDate
  modifiedAt: HelsinkiDateTime
  modifiedByStaff: boolean
}

/**
* Generated from fi.espoo.evaka.absence.application.AbsenceApplicationCreateRequest
*/
export interface AbsenceApplicationCreateRequest {
  childId: PersonId
  description: string
  endDate: LocalDate
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.absence.application.AbsenceApplicationStatus
*/
export type AbsenceApplicationStatus =
  | 'WAITING_DECISION'
  | 'ACCEPTED'
  | 'REJECTED'

/**
* Generated from fi.espoo.evaka.absence.application.AbsenceApplicationStatusUpdateRequest
*/
export interface AbsenceApplicationStatusUpdateRequest {
  reason: string | null
  status: AbsenceApplicationStatus
}

/**
* Generated from fi.espoo.evaka.absence.application.AbsenceApplicationSummary
*/
export interface AbsenceApplicationSummary {
  child: PersonNameDetails
  createdAt: HelsinkiDateTime
  createdBy: EvakaUser
  decidedAt: HelsinkiDateTime | null
  decidedBy: EvakaUser | null
  description: string
  endDate: LocalDate
  id: AbsenceApplicationId
  rejectedReason: string | null
  startDate: LocalDate
  status: AbsenceApplicationStatus
}

/**
* Generated from fi.espoo.evaka.absence.application.AbsenceApplicationSummaryCitizen
*/
export interface AbsenceApplicationSummaryCitizen {
  actions: Action.Citizen.AbsenceApplication[]
  data: AbsenceApplicationSummary
}

/**
* Generated from fi.espoo.evaka.absence.application.AbsenceApplicationSummaryEmployee
*/
export interface AbsenceApplicationSummaryEmployee {
  actions: Action.AbsenceApplication[]
  data: AbsenceApplicationSummary
}

/**
* Generated from fi.espoo.evaka.absence.AbsenceCategory
*/
export type AbsenceCategory =
  | 'BILLABLE'
  | 'NONBILLABLE'

/**
* Generated from fi.espoo.evaka.absence.AbsenceType
*/
export type AbsenceType =
  | 'OTHER_ABSENCE'
  | 'SICKLEAVE'
  | 'PLANNED_ABSENCE'
  | 'UNKNOWN_ABSENCE'
  | 'FORCE_MAJEURE'
  | 'PARENTLEAVE'
  | 'FREE_ABSENCE'
  | 'UNAUTHORIZED_ABSENCE'

/**
* Generated from fi.espoo.evaka.absence.AbsenceUpsert
*/
export interface AbsenceUpsert {
  absenceType: AbsenceType
  category: AbsenceCategory
  childId: PersonId
  date: LocalDate
}

/**
* Generated from fi.espoo.evaka.absence.AbsenceWithModifierInfo
*/
export interface AbsenceWithModifierInfo {
  absenceType: AbsenceType
  category: AbsenceCategory
  modifiedAt: HelsinkiDateTime
  modifiedByStaff: boolean
}

/**
* Generated from fi.espoo.evaka.absence.ChildReservation
*/
export interface ChildReservation {
  created: HelsinkiDateTime
  createdByEvakaUserType: EvakaUserType
  reservation: Reservation
}

/**
* Generated from fi.espoo.evaka.absence.ChildServiceNeedInfo
*/
export interface ChildServiceNeedInfo {
  childId: PersonId
  daycareHoursPerMonth: number | null
  hasContractDays: boolean
  optionName: string
  partWeek: boolean
  shiftCare: ShiftCareType
  validDuring: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.absence.GroupMonthCalendar
*/
export interface GroupMonthCalendar {
  children: GroupMonthCalendarChild[]
  daycareName: string
  daycareOperationTimes: (TimeRange | null)[]
  days: GroupMonthCalendarDay[]
  groupId: GroupId
  groupName: string
  shiftCareOperationTimes: (TimeRange | null)[] | null
}

/**
* Generated from fi.espoo.evaka.absence.GroupMonthCalendarChild
*/
export interface GroupMonthCalendarChild {
  actualServiceNeeds: ChildServiceNeedInfo[]
  attendanceTotalHours: number
  dateOfBirth: LocalDate
  firstName: string
  id: PersonId
  lastName: string
  reservationTotalHours: number
  usedService: UsedServiceTotals | null
}

/**
* Generated from fi.espoo.evaka.absence.GroupMonthCalendarDay
*/
export interface GroupMonthCalendarDay {
  children: GroupMonthCalendarDayChild[]
  date: LocalDate
  isInHolidayPeriod: boolean
  isOperationDay: boolean
}

/**
* Generated from fi.espoo.evaka.absence.GroupMonthCalendarDayChild
*/
export interface GroupMonthCalendarDayChild {
  absenceCategories: AbsenceCategory[]
  absences: AbsenceWithModifierInfo[]
  backupCare: boolean
  childId: PersonId
  dailyServiceTimes: ServiceTimesPresenceStatus
  missingHolidayReservation: boolean
  reservations: ChildReservation[]
  scheduleType: ScheduleType
  shiftCare: ShiftCareType
}

/**
* Generated from fi.espoo.evaka.absence.AbsenceController.HolidayReservationsDelete
*/
export interface HolidayReservationsDelete {
  childId: PersonId
  date: LocalDate
}

/**
* Generated from fi.espoo.evaka.absence.Presence
*/
export interface Presence {
  category: AbsenceCategory
  childId: PersonId
  date: LocalDate
}

/**
* Generated from fi.espoo.evaka.absence.UsedServiceTotals
*/
export interface UsedServiceTotals {
  reservedHours: number
  serviceNeedHours: number
  usedServiceHours: number
}


export function deserializeJsonAbsence(json: JsonOf<Absence>): Absence {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt)
  }
}


export function deserializeJsonAbsenceApplicationCreateRequest(json: JsonOf<AbsenceApplicationCreateRequest>): AbsenceApplicationCreateRequest {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonAbsenceApplicationSummary(json: JsonOf<AbsenceApplicationSummary>): AbsenceApplicationSummary {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    decidedAt: (json.decidedAt != null) ? HelsinkiDateTime.parseIso(json.decidedAt) : null,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonAbsenceApplicationSummaryCitizen(json: JsonOf<AbsenceApplicationSummaryCitizen>): AbsenceApplicationSummaryCitizen {
  return {
    ...json,
    data: deserializeJsonAbsenceApplicationSummary(json.data)
  }
}


export function deserializeJsonAbsenceApplicationSummaryEmployee(json: JsonOf<AbsenceApplicationSummaryEmployee>): AbsenceApplicationSummaryEmployee {
  return {
    ...json,
    data: deserializeJsonAbsenceApplicationSummary(json.data)
  }
}


export function deserializeJsonAbsenceUpsert(json: JsonOf<AbsenceUpsert>): AbsenceUpsert {
  return {
    ...json,
    date: LocalDate.parseIso(json.date)
  }
}


export function deserializeJsonAbsenceWithModifierInfo(json: JsonOf<AbsenceWithModifierInfo>): AbsenceWithModifierInfo {
  return {
    ...json,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt)
  }
}


export function deserializeJsonChildReservation(json: JsonOf<ChildReservation>): ChildReservation {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created),
    reservation: deserializeJsonReservation(json.reservation)
  }
}


export function deserializeJsonChildServiceNeedInfo(json: JsonOf<ChildServiceNeedInfo>): ChildServiceNeedInfo {
  return {
    ...json,
    validDuring: FiniteDateRange.parseJson(json.validDuring)
  }
}


export function deserializeJsonGroupMonthCalendar(json: JsonOf<GroupMonthCalendar>): GroupMonthCalendar {
  return {
    ...json,
    children: json.children.map(e => deserializeJsonGroupMonthCalendarChild(e)),
    daycareOperationTimes: json.daycareOperationTimes.map(e => (e != null) ? TimeRange.parseJson(e) : null),
    days: json.days.map(e => deserializeJsonGroupMonthCalendarDay(e)),
    shiftCareOperationTimes: (json.shiftCareOperationTimes != null) ? json.shiftCareOperationTimes.map(e => (e != null) ? TimeRange.parseJson(e) : null) : null
  }
}


export function deserializeJsonGroupMonthCalendarChild(json: JsonOf<GroupMonthCalendarChild>): GroupMonthCalendarChild {
  return {
    ...json,
    actualServiceNeeds: json.actualServiceNeeds.map(e => deserializeJsonChildServiceNeedInfo(e)),
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}


export function deserializeJsonGroupMonthCalendarDay(json: JsonOf<GroupMonthCalendarDay>): GroupMonthCalendarDay {
  return {
    ...json,
    children: json.children.map(e => deserializeJsonGroupMonthCalendarDayChild(e)),
    date: LocalDate.parseIso(json.date)
  }
}


export function deserializeJsonGroupMonthCalendarDayChild(json: JsonOf<GroupMonthCalendarDayChild>): GroupMonthCalendarDayChild {
  return {
    ...json,
    absences: json.absences.map(e => deserializeJsonAbsenceWithModifierInfo(e)),
    dailyServiceTimes: deserializeJsonServiceTimesPresenceStatus(json.dailyServiceTimes),
    reservations: json.reservations.map(e => deserializeJsonChildReservation(e))
  }
}


export function deserializeJsonHolidayReservationsDelete(json: JsonOf<HolidayReservationsDelete>): HolidayReservationsDelete {
  return {
    ...json,
    date: LocalDate.parseIso(json.date)
  }
}


export function deserializeJsonPresence(json: JsonOf<Presence>): Presence {
  return {
    ...json,
    date: LocalDate.parseIso(json.date)
  }
}
