// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { AbsenceCategory } from './absence'
import type { AbsenceType } from './absence'
import type { ChildImageId } from './shared'
import type { ChildServiceNeedInfo } from './absence'
import type { DailyServiceTimesValue } from './dailyservicetimes'
import type { DaycareId } from './shared'
import type { EvakaUser } from './user'
import FiniteDateRange from '../../finite-date-range'
import type { GroupId } from './shared'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { HolidayPeriodEffect } from './holidayperiod'
import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'
import type { OngoingAttendanceWithUnit } from './attendance'
import type { PersonId } from './shared'
import type { PlacementType } from './placement'
import type { ScheduleType } from './placement'
import type { ShiftCareType } from './serviceneed'
import TimeInterval from '../../time-interval'
import TimeRange from '../../time-range'
import { deserializeJsonChildServiceNeedInfo } from './absence'
import { deserializeJsonDailyServiceTimesValue } from './dailyservicetimes'
import { deserializeJsonHolidayPeriodEffect } from './holidayperiod'
import { deserializeJsonOngoingAttendanceWithUnit } from './attendance'

/**
* Generated from evaka.core.reservations.AbsenceInfo
*/
export interface AbsenceInfo {
  editable: boolean
  type: AbsenceType
}

/**
* Generated from evaka.core.reservations.AbsenceRequest
*/
export interface AbsenceRequest {
  absenceType: AbsenceType
  childIds: PersonId[]
  dateRange: FiniteDateRange
}

/**
* Generated from evaka.core.reservations.AbsenceTypeResponse
*/
export interface AbsenceTypeResponse {
  absenceType: AbsenceType
  staffCreated: boolean
}

/**
* Generated from evaka.core.reservations.AttendanceTimesForDate
*/
export interface AttendanceTimesForDate {
  date: LocalDate
  interval: TimeInterval
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUser
}

/**
* Generated from evaka.core.reservations.AttendanceReservationController.BackupPlacementType
*/
export type BackupPlacementType =
  | 'OUT_ON_BACKUP_PLACEMENT'
  | 'IN_BACKUP_PLACEMENT'

/**
* Generated from evaka.core.reservations.UnitAttendanceReservations.Child
*/
export interface Child {
  dateOfBirth: LocalDate
  firstName: string
  id: PersonId
  lastName: string
  preferredName: string
  serviceNeeds: ChildServiceNeedInfo[]
}

/**
* Generated from evaka.core.reservations.ChildDatePresence
*/
export interface ChildDatePresence {
  absenceBillable: AbsenceType | null
  absenceNonbillable: AbsenceType | null
  attendances: TimeInterval[]
  childId: PersonId
  date: LocalDate
  reservations: Reservation[]
  unitId: DaycareId
}

/**
* Generated from evaka.core.reservations.UnitAttendanceReservations.ChildRecordOfDay
*/
export interface ChildRecordOfDay {
  absenceBillable: AbsenceTypeResponse | null
  absenceNonbillable: AbsenceTypeResponse | null
  attendances: AttendanceTimesForDate[]
  backupGroupId: GroupId | null
  childId: PersonId
  dailyServiceTimes: DailyServiceTimesValue | null
  groupId: GroupId | null
  inOtherUnit: boolean
  occupancy: number
  possibleAbsenceCategories: AbsenceCategory[]
  reservations: ReservationResponse[]
  scheduleType: ScheduleType
  shiftCare: ShiftCareType | null
}

/**
* Generated from evaka.core.reservations.AttendanceReservationController.ChildReservationInfo
*/
export interface ChildReservationInfo {
  absent: boolean
  backupPlacement: BackupPlacementType | null
  childId: PersonId
  dailyServiceTimes: DailyServiceTimesValue | null
  groupId: GroupId | null
  isInHolidayPeriod: boolean
  reservations: ReservationResponse[]
  scheduleType: ScheduleType
}

/**
* Generated from evaka.core.reservations.ConfirmedRangeDate
*/
export interface ConfirmedRangeDate {
  absenceType: AbsenceType | null
  dailyServiceTimes: DailyServiceTimesValue | null
  date: LocalDate
  reservations: ReservationResponse[]
  scheduleType: ScheduleType
}

/**
* Generated from evaka.core.reservations.ConfirmedRangeDateUpdate
*/
export interface ConfirmedRangeDateUpdate {
  absenceType: AbsenceType | null
  date: LocalDate
  reservations: Reservation[]
}

/**
* Generated from evaka.core.reservations.AttendanceReservationController.DailyChildReservationResult
*/
export interface DailyChildReservationResult {
  childReservations: ChildReservationInfo[]
  children: Partial<Record<PersonId, ReservationChildInfo>>
}


export namespace DailyReservationRequest {
  /**
  * Generated from evaka.core.reservations.DailyReservationRequest.Absent
  */
  export interface Absent {
    type: 'ABSENT'
    childId: PersonId
    date: LocalDate
  }

  /**
  * Generated from evaka.core.reservations.DailyReservationRequest.Nothing
  */
  export interface Nothing {
    type: 'NOTHING'
    childId: PersonId
    date: LocalDate
  }

  /**
  * Generated from evaka.core.reservations.DailyReservationRequest.Present
  */
  export interface Present {
    type: 'PRESENT'
    childId: PersonId
    date: LocalDate
  }

  /**
  * Generated from evaka.core.reservations.DailyReservationRequest.Reservations
  */
  export interface Reservations {
    type: 'RESERVATIONS'
    childId: PersonId
    date: LocalDate
    reservation: TimeRange
    secondReservation: TimeRange | null
  }
}

/**
* Generated from evaka.core.reservations.DailyReservationRequest
*/
export type DailyReservationRequest = DailyReservationRequest.Absent | DailyReservationRequest.Nothing | DailyReservationRequest.Present | DailyReservationRequest.Reservations


/**
* Generated from evaka.core.reservations.AttendanceReservationController.DayReservationStatisticsResult
*/
export interface DayReservationStatisticsResult {
  date: LocalDate
  groupStatistics: GroupReservationStatisticResult[]
}

/**
* Generated from evaka.core.reservations.AttendanceReservationController.ExpectedAbsencesRequest
*/
export interface ExpectedAbsencesRequest {
  attendances: TimeRange[]
  childId: PersonId
  date: LocalDate
}

/**
* Generated from evaka.core.reservations.AttendanceReservationController.ExpectedAbsencesResponse
*/
export interface ExpectedAbsencesResponse {
  categories: AbsenceCategory[] | null
}

/**
* Generated from evaka.core.reservations.AttendanceReservationController.GroupReservationStatisticResult
*/
export interface GroupReservationStatisticResult {
  absentCount: number
  calculatedPresent: number
  groupId: GroupId | null
  presentCount: number
}

/**
* Generated from evaka.core.reservations.MonthSummary
*/
export interface MonthSummary {
  month: number
  reservedMinutes: number
  serviceNeedMinutes: number
  usedServiceMinutes: number
  year: number
}

/**
* Generated from evaka.core.reservations.AttendanceReservationController.OngoingAttendanceResponse
*/
export interface OngoingAttendanceResponse {
  ongoingAttendance: OngoingAttendanceWithUnit | null
}

/**
* Generated from evaka.core.reservations.ReservationControllerCitizen.OperationalDatesRequest
*/
export interface OperationalDatesRequest {
  childIds: PersonId[]
  range: FiniteDateRange
}

/**
* Generated from evaka.core.reservations.UnitAttendanceReservations.OperationalDay
*/
export interface OperationalDay {
  children: ChildRecordOfDay[]
  date: LocalDate
  dateInfo: UnitDateInfo
}


export namespace ReservableTimeRange {
  /**
  * Generated from evaka.core.reservations.ReservableTimeRange.IntermittentShiftCare
  */
  export interface IntermittentShiftCare {
    type: 'INTERMITTENT_SHIFT_CARE'
    placementUnitOperationTime: TimeRange | null
  }

  /**
  * Generated from evaka.core.reservations.ReservableTimeRange.Normal
  */
  export interface Normal {
    type: 'NORMAL'
    range: TimeRange
  }

  /**
  * Generated from evaka.core.reservations.ReservableTimeRange.ShiftCare
  */
  export interface ShiftCare {
    type: 'SHIFT_CARE'
    range: TimeRange
  }
}

/**
* Generated from evaka.core.reservations.ReservableTimeRange
*/
export type ReservableTimeRange = ReservableTimeRange.IntermittentShiftCare | ReservableTimeRange.Normal | ReservableTimeRange.ShiftCare



export namespace Reservation {
  /**
  * Generated from evaka.core.reservations.Reservation.NoTimes
  */
  export interface NoTimes {
    type: 'NO_TIMES'
  }

  /**
  * Generated from evaka.core.reservations.Reservation.Times
  */
  export interface Times {
    type: 'TIMES'
    range: TimeRange
  }
}

/**
* Generated from evaka.core.reservations.Reservation
*/
export type Reservation = Reservation.NoTimes | Reservation.Times


/**
* Generated from evaka.core.reservations.ReservationChild
*/
export interface ReservationChild {
  duplicateOf: PersonId | null
  firstName: string
  id: PersonId
  imageId: ChildImageId | null
  lastName: string
  monthSummaries: MonthSummary[]
  preferredName: string
  upcomingPlacementStartDate: LocalDate | null
  upcomingPlacementType: PlacementType | null
  upcomingPlacementUnitName: string | null
}

/**
* Generated from evaka.core.reservations.AttendanceReservationController.ReservationChildInfo
*/
export interface ReservationChildInfo {
  dateOfBirth: LocalDate
  firstName: string
  id: PersonId
  lastName: string
  preferredName: string
}

/**
* Generated from evaka.core.reservations.UnitAttendanceReservations.ReservationGroup
*/
export interface ReservationGroup {
  id: GroupId
  name: string
}


export namespace ReservationResponse {
  /**
  * Generated from evaka.core.reservations.ReservationResponse.NoTimes
  */
  export interface NoTimes {
    type: 'NO_TIMES'
    modifiedAt: HelsinkiDateTime | null
    modifiedBy: EvakaUser | null
    staffCreated: boolean
  }

  /**
  * Generated from evaka.core.reservations.ReservationResponse.Times
  */
  export interface Times {
    type: 'TIMES'
    modifiedAt: HelsinkiDateTime | null
    modifiedBy: EvakaUser | null
    range: TimeRange
    staffCreated: boolean
  }
}

/**
* Generated from evaka.core.reservations.ReservationResponse
*/
export type ReservationResponse = ReservationResponse.NoTimes | ReservationResponse.Times


/**
* Generated from evaka.core.reservations.ReservationResponseDay
*/
export interface ReservationResponseDay {
  children: ReservationResponseDayChild[]
  date: LocalDate
  holiday: boolean
}

/**
* Generated from evaka.core.reservations.ReservationResponseDayChild
*/
export interface ReservationResponseDayChild {
  absence: AbsenceInfo | null
  attendances: TimeInterval[]
  childId: PersonId
  holidayPeriodEffect: HolidayPeriodEffect | null
  reservableTimeRange: ReservableTimeRange
  reservations: ReservationResponse[]
  scheduleType: ScheduleType
  shiftCare: boolean
  usedService: UsedServiceResult | null
}

/**
* Generated from evaka.core.reservations.ReservationsResponse
*/
export interface ReservationsResponse {
  children: ReservationChild[]
  days: ReservationResponseDay[]
  reservableRange: FiniteDateRange
}

/**
* Generated from evaka.core.reservations.UnitAttendanceReservations
*/
export interface UnitAttendanceReservations {
  children: Child[]
  days: OperationalDay[]
  groups: ReservationGroup[]
  unit: string
}

/**
* Generated from evaka.core.reservations.UnitAttendanceReservations.UnitDateInfo
*/
export interface UnitDateInfo {
  isHoliday: boolean
  isInHolidayPeriod: boolean
  normalOperatingTimes: TimeRange | null
  shiftCareOpenOnHoliday: boolean
  shiftCareOperatingTimes: TimeRange | null
}

/**
* Generated from evaka.core.reservations.UsedServiceResult
*/
export interface UsedServiceResult {
  reservedMinutes: number
  usedServiceMinutes: number
  usedServiceRanges: TimeRange[]
}


export function deserializeJsonAbsenceRequest(json: JsonOf<AbsenceRequest>): AbsenceRequest {
  return {
    ...json,
    dateRange: FiniteDateRange.parseJson(json.dateRange)
  }
}


export function deserializeJsonAttendanceTimesForDate(json: JsonOf<AttendanceTimesForDate>): AttendanceTimesForDate {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    interval: TimeInterval.parseJson(json.interval),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt)
  }
}


export function deserializeJsonChild(json: JsonOf<Child>): Child {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    serviceNeeds: json.serviceNeeds.map(e => deserializeJsonChildServiceNeedInfo(e))
  }
}


export function deserializeJsonChildDatePresence(json: JsonOf<ChildDatePresence>): ChildDatePresence {
  return {
    ...json,
    attendances: json.attendances.map(e => TimeInterval.parseJson(e)),
    date: LocalDate.parseIso(json.date),
    reservations: json.reservations.map(e => deserializeJsonReservation(e))
  }
}


export function deserializeJsonChildRecordOfDay(json: JsonOf<ChildRecordOfDay>): ChildRecordOfDay {
  return {
    ...json,
    attendances: json.attendances.map(e => deserializeJsonAttendanceTimesForDate(e)),
    dailyServiceTimes: (json.dailyServiceTimes != null) ? deserializeJsonDailyServiceTimesValue(json.dailyServiceTimes) : null,
    reservations: json.reservations.map(e => deserializeJsonReservationResponse(e))
  }
}


export function deserializeJsonChildReservationInfo(json: JsonOf<ChildReservationInfo>): ChildReservationInfo {
  return {
    ...json,
    dailyServiceTimes: (json.dailyServiceTimes != null) ? deserializeJsonDailyServiceTimesValue(json.dailyServiceTimes) : null,
    reservations: json.reservations.map(e => deserializeJsonReservationResponse(e))
  }
}


export function deserializeJsonConfirmedRangeDate(json: JsonOf<ConfirmedRangeDate>): ConfirmedRangeDate {
  return {
    ...json,
    dailyServiceTimes: (json.dailyServiceTimes != null) ? deserializeJsonDailyServiceTimesValue(json.dailyServiceTimes) : null,
    date: LocalDate.parseIso(json.date),
    reservations: json.reservations.map(e => deserializeJsonReservationResponse(e))
  }
}


export function deserializeJsonConfirmedRangeDateUpdate(json: JsonOf<ConfirmedRangeDateUpdate>): ConfirmedRangeDateUpdate {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    reservations: json.reservations.map(e => deserializeJsonReservation(e))
  }
}


export function deserializeJsonDailyChildReservationResult(json: JsonOf<DailyChildReservationResult>): DailyChildReservationResult {
  return {
    ...json,
    childReservations: json.childReservations.map(e => deserializeJsonChildReservationInfo(e)),
    children: Object.fromEntries(Object.entries(json.children).map(
      ([k, v]) => [k, v !== undefined ? deserializeJsonReservationChildInfo(v) : v]
    ))
  }
}



export function deserializeJsonDailyReservationRequestAbsent(json: JsonOf<DailyReservationRequest.Absent>): DailyReservationRequest.Absent {
  return {
    ...json,
    date: LocalDate.parseIso(json.date)
  }
}

export function deserializeJsonDailyReservationRequestNothing(json: JsonOf<DailyReservationRequest.Nothing>): DailyReservationRequest.Nothing {
  return {
    ...json,
    date: LocalDate.parseIso(json.date)
  }
}

export function deserializeJsonDailyReservationRequestPresent(json: JsonOf<DailyReservationRequest.Present>): DailyReservationRequest.Present {
  return {
    ...json,
    date: LocalDate.parseIso(json.date)
  }
}

export function deserializeJsonDailyReservationRequestReservations(json: JsonOf<DailyReservationRequest.Reservations>): DailyReservationRequest.Reservations {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    reservation: TimeRange.parseJson(json.reservation),
    secondReservation: (json.secondReservation != null) ? TimeRange.parseJson(json.secondReservation) : null
  }
}
export function deserializeJsonDailyReservationRequest(json: JsonOf<DailyReservationRequest>): DailyReservationRequest {
  switch (json.type) {
    case 'ABSENT': return deserializeJsonDailyReservationRequestAbsent(json)
    case 'NOTHING': return deserializeJsonDailyReservationRequestNothing(json)
    case 'PRESENT': return deserializeJsonDailyReservationRequestPresent(json)
    case 'RESERVATIONS': return deserializeJsonDailyReservationRequestReservations(json)
    default: return json
  }
}


export function deserializeJsonDayReservationStatisticsResult(json: JsonOf<DayReservationStatisticsResult>): DayReservationStatisticsResult {
  return {
    ...json,
    date: LocalDate.parseIso(json.date)
  }
}


export function deserializeJsonExpectedAbsencesRequest(json: JsonOf<ExpectedAbsencesRequest>): ExpectedAbsencesRequest {
  return {
    ...json,
    attendances: json.attendances.map(e => TimeRange.parseJson(e)),
    date: LocalDate.parseIso(json.date)
  }
}


export function deserializeJsonOngoingAttendanceResponse(json: JsonOf<OngoingAttendanceResponse>): OngoingAttendanceResponse {
  return {
    ...json,
    ongoingAttendance: (json.ongoingAttendance != null) ? deserializeJsonOngoingAttendanceWithUnit(json.ongoingAttendance) : null
  }
}


export function deserializeJsonOperationalDatesRequest(json: JsonOf<OperationalDatesRequest>): OperationalDatesRequest {
  return {
    ...json,
    range: FiniteDateRange.parseJson(json.range)
  }
}


export function deserializeJsonOperationalDay(json: JsonOf<OperationalDay>): OperationalDay {
  return {
    ...json,
    children: json.children.map(e => deserializeJsonChildRecordOfDay(e)),
    date: LocalDate.parseIso(json.date),
    dateInfo: deserializeJsonUnitDateInfo(json.dateInfo)
  }
}



export function deserializeJsonReservableTimeRangeIntermittentShiftCare(json: JsonOf<ReservableTimeRange.IntermittentShiftCare>): ReservableTimeRange.IntermittentShiftCare {
  return {
    ...json,
    placementUnitOperationTime: (json.placementUnitOperationTime != null) ? TimeRange.parseJson(json.placementUnitOperationTime) : null
  }
}

export function deserializeJsonReservableTimeRangeNormal(json: JsonOf<ReservableTimeRange.Normal>): ReservableTimeRange.Normal {
  return {
    ...json,
    range: TimeRange.parseJson(json.range)
  }
}

export function deserializeJsonReservableTimeRangeShiftCare(json: JsonOf<ReservableTimeRange.ShiftCare>): ReservableTimeRange.ShiftCare {
  return {
    ...json,
    range: TimeRange.parseJson(json.range)
  }
}
export function deserializeJsonReservableTimeRange(json: JsonOf<ReservableTimeRange>): ReservableTimeRange {
  switch (json.type) {
    case 'INTERMITTENT_SHIFT_CARE': return deserializeJsonReservableTimeRangeIntermittentShiftCare(json)
    case 'NORMAL': return deserializeJsonReservableTimeRangeNormal(json)
    case 'SHIFT_CARE': return deserializeJsonReservableTimeRangeShiftCare(json)
    default: return json
  }
}



export function deserializeJsonReservationTimes(json: JsonOf<Reservation.Times>): Reservation.Times {
  return {
    ...json,
    range: TimeRange.parseJson(json.range)
  }
}
export function deserializeJsonReservation(json: JsonOf<Reservation>): Reservation {
  switch (json.type) {
    case 'TIMES': return deserializeJsonReservationTimes(json)
    default: return json
  }
}


export function deserializeJsonReservationChild(json: JsonOf<ReservationChild>): ReservationChild {
  return {
    ...json,
    upcomingPlacementStartDate: (json.upcomingPlacementStartDate != null) ? LocalDate.parseIso(json.upcomingPlacementStartDate) : null
  }
}


export function deserializeJsonReservationChildInfo(json: JsonOf<ReservationChildInfo>): ReservationChildInfo {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}



export function deserializeJsonReservationResponseNoTimes(json: JsonOf<ReservationResponse.NoTimes>): ReservationResponse.NoTimes {
  return {
    ...json,
    modifiedAt: (json.modifiedAt != null) ? HelsinkiDateTime.parseIso(json.modifiedAt) : null
  }
}

export function deserializeJsonReservationResponseTimes(json: JsonOf<ReservationResponse.Times>): ReservationResponse.Times {
  return {
    ...json,
    modifiedAt: (json.modifiedAt != null) ? HelsinkiDateTime.parseIso(json.modifiedAt) : null,
    range: TimeRange.parseJson(json.range)
  }
}
export function deserializeJsonReservationResponse(json: JsonOf<ReservationResponse>): ReservationResponse {
  switch (json.type) {
    case 'NO_TIMES': return deserializeJsonReservationResponseNoTimes(json)
    case 'TIMES': return deserializeJsonReservationResponseTimes(json)
    default: return json
  }
}


export function deserializeJsonReservationResponseDay(json: JsonOf<ReservationResponseDay>): ReservationResponseDay {
  return {
    ...json,
    children: json.children.map(e => deserializeJsonReservationResponseDayChild(e)),
    date: LocalDate.parseIso(json.date)
  }
}


export function deserializeJsonReservationResponseDayChild(json: JsonOf<ReservationResponseDayChild>): ReservationResponseDayChild {
  return {
    ...json,
    attendances: json.attendances.map(e => TimeInterval.parseJson(e)),
    holidayPeriodEffect: (json.holidayPeriodEffect != null) ? deserializeJsonHolidayPeriodEffect(json.holidayPeriodEffect) : null,
    reservableTimeRange: deserializeJsonReservableTimeRange(json.reservableTimeRange),
    reservations: json.reservations.map(e => deserializeJsonReservationResponse(e)),
    usedService: (json.usedService != null) ? deserializeJsonUsedServiceResult(json.usedService) : null
  }
}


export function deserializeJsonReservationsResponse(json: JsonOf<ReservationsResponse>): ReservationsResponse {
  return {
    ...json,
    children: json.children.map(e => deserializeJsonReservationChild(e)),
    days: json.days.map(e => deserializeJsonReservationResponseDay(e)),
    reservableRange: FiniteDateRange.parseJson(json.reservableRange)
  }
}


export function deserializeJsonUnitAttendanceReservations(json: JsonOf<UnitAttendanceReservations>): UnitAttendanceReservations {
  return {
    ...json,
    children: json.children.map(e => deserializeJsonChild(e)),
    days: json.days.map(e => deserializeJsonOperationalDay(e))
  }
}


export function deserializeJsonUnitDateInfo(json: JsonOf<UnitDateInfo>): UnitDateInfo {
  return {
    ...json,
    normalOperatingTimes: (json.normalOperatingTimes != null) ? TimeRange.parseJson(json.normalOperatingTimes) : null,
    shiftCareOperatingTimes: (json.shiftCareOperatingTimes != null) ? TimeRange.parseJson(json.shiftCareOperatingTimes) : null
  }
}


export function deserializeJsonUsedServiceResult(json: JsonOf<UsedServiceResult>): UsedServiceResult {
  return {
    ...json,
    usedServiceRanges: json.usedServiceRanges.map(e => TimeRange.parseJson(e))
  }
}
