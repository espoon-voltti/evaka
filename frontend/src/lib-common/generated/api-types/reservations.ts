// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import FiniteDateRange from '../../finite-date-range'
import LocalDate from '../../local-date'
import LocalTime from '../../local-time'
import { AbsenceCategory } from './daycare'
import { AbsenceType } from './daycare'
import { ChildServiceNeedInfo } from './daycare'
import { DailyServiceTimesValue } from './dailyservicetimes'
import { PlacementType } from './placement'
import { ScheduleType } from './placement'
import { TimeRange } from './shared'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.reservations.UnitAttendanceReservations.Absence
*/
export interface Absence {
  category: AbsenceCategory
  type: AbsenceType
}

/**
* Generated from fi.espoo.evaka.reservations.AbsenceInfo
*/
export interface AbsenceInfo {
  editable: boolean
  type: AbsenceType
}

/**
* Generated from fi.espoo.evaka.reservations.AbsenceInput
*/
export interface AbsenceInput {
  category: AbsenceCategory
  type: AbsenceType
}

/**
* Generated from fi.espoo.evaka.reservations.AbsenceRequest
*/
export interface AbsenceRequest {
  absenceType: AbsenceType
  childIds: UUID[]
  dateRange: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.reservations.UnitAttendanceReservations.AttendanceTimes
*/
export interface AttendanceTimes {
  endTime: LocalTime | null
  startTime: LocalTime
}

/**
* Generated from fi.espoo.evaka.reservations.UnitAttendanceReservations.Child
*/
export interface Child {
  dateOfBirth: LocalDate
  firstName: string
  id: UUID
  lastName: string
  preferredName: string
  serviceNeeds: ChildServiceNeedInfo[]
}

/**
* Generated from fi.espoo.evaka.reservations.ChildDatePresence
*/
export interface ChildDatePresence {
  absences: AbsenceInput[]
  attendances: OpenTimeRange[]
  childId: UUID
  date: LocalDate
  reservations: Reservation[]
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.reservations.UnitAttendanceReservations.ChildRecordOfDay
*/
export interface ChildRecordOfDay {
  absences: Absence[]
  attendances: AttendanceTimes[]
  backupGroupId: UUID | null
  childId: UUID
  dailyServiceTimes: DailyServiceTimesValue | null
  groupId: UUID | null
  inOtherUnit: boolean
  reservations: Reservation[]
  scheduleType: ScheduleType
}

/**
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.ChildReservationInfo
*/
export interface ChildReservationInfo {
  absent: boolean
  childId: UUID
  dailyServiceTimes: DailyServiceTimesValue | null
  groupId: UUID | null
  isInHolidayPeriod: boolean
  onTermBreak: boolean
  outOnBackupPlacement: boolean
  reservations: Reservation[]
}

/**
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.DailyChildReservationResult
*/
export interface DailyChildReservationResult {
  childReservations: ChildReservationInfo[]
  children: Record<string, ReservationChildInfo>
}

export namespace DailyReservationRequest {
  /**
  * Generated from fi.espoo.evaka.reservations.DailyReservationRequest.Absent
  */
  export interface Absent {
    type: 'ABSENT'
    childId: UUID
    date: LocalDate
  }
  
  /**
  * Generated from fi.espoo.evaka.reservations.DailyReservationRequest.Nothing
  */
  export interface Nothing {
    type: 'NOTHING'
    childId: UUID
    date: LocalDate
  }
  
  /**
  * Generated from fi.espoo.evaka.reservations.DailyReservationRequest.Present
  */
  export interface Present {
    type: 'PRESENT'
    childId: UUID
    date: LocalDate
  }
  
  /**
  * Generated from fi.espoo.evaka.reservations.DailyReservationRequest.Reservations
  */
  export interface Reservations {
    type: 'RESERVATIONS'
    childId: UUID
    date: LocalDate
    reservation: TimeRange
    secondReservation: TimeRange | null
  }
}

/**
* Generated from fi.espoo.evaka.reservations.DailyReservationRequest
*/
export type DailyReservationRequest = DailyReservationRequest.Absent | DailyReservationRequest.Nothing | DailyReservationRequest.Present | DailyReservationRequest.Reservations


/**
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.DayReservationStatisticsResult
*/
export interface DayReservationStatisticsResult {
  date: LocalDate
  groupStatistics: GroupReservationStatisticResult[]
}

/**
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.GroupReservationStatisticResult
*/
export interface GroupReservationStatisticResult {
  absentCount: number
  calculatedPresent: number
  groupId: UUID | null
  presentCount: number
}

/**
* Generated from fi.espoo.evaka.reservations.NonReservableReservation
*/
export interface NonReservableReservation {
  absenceType: AbsenceType | null
  dailyServiceTimes: DailyServiceTimesValue | null
  date: LocalDate
  reservations: Reservation[]
}

/**
* Generated from fi.espoo.evaka.reservations.OpenTimeRange
*/
export interface OpenTimeRange {
  endTime: LocalTime | null
  startTime: LocalTime
}

/**
* Generated from fi.espoo.evaka.reservations.UnitAttendanceReservations.OperationalDay
*/
export interface OperationalDay {
  children: ChildRecordOfDay[]
  date: LocalDate
  dateInfo: UnitDateInfo
}

export namespace ReservableTimeRange {
  /**
  * Generated from fi.espoo.evaka.reservations.ReservableTimeRange.IntermittentShiftCare
  */
  export interface IntermittentShiftCare {
    type: 'INTERMITTENT_SHIFT_CARE'
    placementUnitOperationTime: TimeRange | null
  }
  
  /**
  * Generated from fi.espoo.evaka.reservations.ReservableTimeRange.Normal
  */
  export interface Normal {
    type: 'NORMAL'
    range: TimeRange
  }
}

/**
* Generated from fi.espoo.evaka.reservations.ReservableTimeRange
*/
export type ReservableTimeRange = ReservableTimeRange.IntermittentShiftCare | ReservableTimeRange.Normal


export namespace Reservation {
  /**
  * Generated from fi.espoo.evaka.reservations.Reservation.NoTimes
  */
  export interface NoTimes {
    type: 'NO_TIMES'
  }
  
  /**
  * Generated from fi.espoo.evaka.reservations.Reservation.Times
  */
  export interface Times {
    type: 'TIMES'
    endTime: LocalTime
    startTime: LocalTime
  }
}

/**
* Generated from fi.espoo.evaka.reservations.Reservation
*/
export type Reservation = Reservation.NoTimes | Reservation.Times


/**
* Generated from fi.espoo.evaka.reservations.ReservationChild
*/
export interface ReservationChild {
  duplicateOf: UUID | null
  firstName: string
  id: UUID
  imageId: UUID | null
  lastName: string
  preferredName: string
  upcomingPlacementType: PlacementType | null
}

/**
* Generated from fi.espoo.evaka.reservations.AttendanceReservationController.ReservationChildInfo
*/
export interface ReservationChildInfo {
  dateOfBirth: LocalDate
  firstName: string
  id: UUID
  lastName: string
  preferredName: string
}

/**
* Generated from fi.espoo.evaka.reservations.UnitAttendanceReservations.ReservationGroup
*/
export interface ReservationGroup {
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.reservations.ReservationResponseDay
*/
export interface ReservationResponseDay {
  children: ReservationResponseDayChild[]
  date: LocalDate
  holiday: boolean
}

/**
* Generated from fi.espoo.evaka.reservations.ReservationResponseDayChild
*/
export interface ReservationResponseDayChild {
  absence: AbsenceInfo | null
  attendances: OpenTimeRange[]
  childId: UUID
  reservableTimeRange: ReservableTimeRange
  reservations: Reservation[]
  scheduleType: ScheduleType
  shiftCare: boolean
}

/**
* Generated from fi.espoo.evaka.reservations.ReservationsResponse
*/
export interface ReservationsResponse {
  children: ReservationChild[]
  days: ReservationResponseDay[]
  reservableRange: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.reservations.UnitAttendanceReservations
*/
export interface UnitAttendanceReservations {
  children: Child[]
  days: OperationalDay[]
  groups: ReservationGroup[]
  unit: string
}

/**
* Generated from fi.espoo.evaka.reservations.UnitAttendanceReservations.UnitDateInfo
*/
export interface UnitDateInfo {
  isHoliday: boolean
  isInHolidayPeriod: boolean
  time: TimeRange | null
}
