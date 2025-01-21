// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import LocalTime from '../../local-time'
import { AbsenceCategory } from './absence'
import { AbsenceType } from './absence'
import { ChildDailyNote } from './note'
import { ChildStickyNote } from './note'
import { DailyServiceTimesValue } from './dailyservicetimes'
import { DaycareId } from './shared'
import { EmployeeId } from './shared'
import { EvakaUser } from './user'
import { GroupId } from './shared'
import { HelsinkiDateTimeRange } from './shared'
import { JsonOf } from '../../json'
import { PersonId } from './shared'
import { PilotFeature } from './shared'
import { PlacementType } from './placement'
import { ReservationResponse } from './reservations'
import { ScheduleType } from './placement'
import { StaffAttendanceExternalId } from './shared'
import { StaffAttendanceRealtimeId } from './shared'
import { deserializeJsonChildDailyNote } from './note'
import { deserializeJsonChildStickyNote } from './note'
import { deserializeJsonDailyServiceTimesValue } from './dailyservicetimes'
import { deserializeJsonHelsinkiDateTimeRange } from './shared'
import { deserializeJsonReservationResponse } from './reservations'

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.AbsenceRangeRequest
*/
export interface AbsenceRangeRequest {
  absenceType: AbsenceType
  range: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.ArrivalsRequest
*/
export interface ArrivalsRequest {
  arrived: LocalTime
  children: PersonId[]
}

/**
* Generated from fi.espoo.evaka.attendance.Attendance
*/
export interface Attendance {
  arrived: HelsinkiDateTime
  arrivedAddedAt: HelsinkiDateTime | null
  arrivedAddedBy: EvakaUser | null
  arrivedModifiedAt: HelsinkiDateTime | null
  arrivedModifiedBy: EvakaUser | null
  departed: HelsinkiDateTime | null
  departedAddedAt: HelsinkiDateTime | null
  departedAddedBy: EvakaUser | null
  departedAutomatically: boolean
  departedModifiedAt: HelsinkiDateTime | null
  departedModifiedBy: EvakaUser | null
  groupId: GroupId | null
  id: StaffAttendanceRealtimeId
  occupancyCoefficient: number
  type: StaffAttendanceType
}

/**
* Generated from fi.espoo.evaka.attendance.AttendanceChild
*/
export interface AttendanceChild {
  backup: boolean
  dailyNote: ChildDailyNote | null
  dailyServiceTimes: DailyServiceTimesValue | null
  dateOfBirth: LocalDate
  firstName: string
  groupId: GroupId | null
  id: PersonId
  imageUrl: string | null
  lastName: string
  operationalDates: LocalDate[]
  placementType: PlacementType
  preferredName: string
  reservations: ReservationResponse[]
  scheduleType: ScheduleType
  stickyNotes: ChildStickyNote[]
}

/**
* Generated from fi.espoo.evaka.attendance.AttendanceStatus
*/
export type AttendanceStatus =
  | 'COMING'
  | 'PRESENT'
  | 'DEPARTED'
  | 'ABSENT'

/**
* Generated from fi.espoo.evaka.attendance.AttendanceTimes
*/
export interface AttendanceTimes {
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUser
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAbsence
*/
export interface ChildAbsence {
  category: AbsenceCategory
  type: AbsenceType
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.ChildAttendanceStatusResponse
*/
export interface ChildAttendanceStatusResponse {
  absences: ChildAbsence[]
  attendances: AttendanceTimes[]
  status: AttendanceStatus
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.ChildDeparture
*/
export interface ChildDeparture {
  absenceTypeBillable: AbsenceType | null
  absenceTypeNonbillable: AbsenceType | null
  childId: PersonId
}

/**
* Generated from fi.espoo.evaka.attendance.ContactInfo
*/
export interface ContactInfo {
  backupPhone: string
  email: string
  firstName: string
  id: string
  lastName: string
  phone: string
  priority: number | null
}

/**
* Generated from fi.espoo.evaka.attendance.CurrentDayStaffAttendanceResponse
*/
export interface CurrentDayStaffAttendanceResponse {
  extraAttendances: ExternalStaffMember[]
  staff: StaffMember[]
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.DeparturesRequest
*/
export interface DeparturesRequest {
  departed: LocalTime
  departures: ChildDeparture[]
}

/**
* Generated from fi.espoo.evaka.attendance.EmployeeAttendance
*/
export interface EmployeeAttendance {
  allowedToEdit: boolean
  attendances: Attendance[]
  currentOccupancyCoefficient: number
  employeeId: EmployeeId
  firstName: string
  groups: GroupId[]
  lastName: string
  plannedAttendances: PlannedStaffAttendance[]
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.ExpectedAbsencesOnDeparturesRequest
*/
export interface ExpectedAbsencesOnDeparturesRequest {
  childIds: PersonId[]
  departed: LocalTime
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.ExpectedAbsencesOnDeparturesResponse
*/
export interface ExpectedAbsencesOnDeparturesResponse {
  categoriesByChild: Partial<Record<PersonId, AbsenceCategory[] | null>>
}

/**
* Generated from fi.espoo.evaka.attendance.ExternalAttendance
*/
export interface ExternalAttendance {
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
  departedAutomatically: boolean
  groupId: GroupId
  id: StaffAttendanceExternalId
  name: string
  occupancyCoefficient: number
  type: StaffAttendanceType
}

/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.ExternalAttendanceBody
*/
export interface ExternalAttendanceBody {
  date: LocalDate
  entries: ExternalAttendanceUpsert[]
  name: string
  unitId: DaycareId
}

/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.ExternalAttendanceUpsert
*/
export interface ExternalAttendanceUpsert {
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
  groupId: GroupId
  hasStaffOccupancyEffect: boolean
  id: StaffAttendanceExternalId | null
}

/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.ExternalStaffArrivalRequest
*/
export interface ExternalStaffArrivalRequest {
  arrived: LocalTime
  groupId: GroupId
  hasStaffOccupancyEffect: boolean
  name: string
}

/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.ExternalStaffDepartureRequest
*/
export interface ExternalStaffDepartureRequest {
  attendanceId: StaffAttendanceExternalId
  time: LocalTime
}

/**
* Generated from fi.espoo.evaka.attendance.ExternalStaffMember
*/
export interface ExternalStaffMember {
  arrived: HelsinkiDateTime
  groupId: GroupId
  id: StaffAttendanceExternalId
  name: string
  occupancyEffect: boolean
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.FullDayAbsenceRequest
*/
export interface FullDayAbsenceRequest {
  absenceType: AbsenceType
}

/**
* Generated from fi.espoo.evaka.attendance.GroupInfo
*/
export interface GroupInfo {
  id: GroupId
  name: string
  utilization: number
}

/**
* Generated from fi.espoo.evaka.attendance.OpenGroupAttendance
*/
export interface OpenGroupAttendance {
  date: LocalDate
  groupId: GroupId
  unitId: DaycareId
  unitName: string
}

/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.OpenGroupAttendanceResponse
*/
export interface OpenGroupAttendanceResponse {
  openGroupAttendance: OpenGroupAttendance | null
}

/**
* Generated from fi.espoo.evaka.attendance.PlannedStaffAttendance
*/
export interface PlannedStaffAttendance {
  end: HelsinkiDateTime
  start: HelsinkiDateTime
  type: StaffAttendanceType
}

/**
* Generated from fi.espoo.evaka.attendance.Staff
*/
export interface Staff {
  firstName: string
  groups: GroupId[]
  id: EmployeeId
  lastName: string
  pinLocked: boolean
  pinSet: boolean
}

/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.StaffArrivalRequest
*/
export interface StaffArrivalRequest {
  employeeId: EmployeeId
  groupId: GroupId
  hasStaffOccupancyEffect: boolean | null
  pinCode: string
  time: LocalTime
  type: StaffAttendanceType | null
}

/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.StaffAttendanceBody
*/
export interface StaffAttendanceBody {
  date: LocalDate
  employeeId: EmployeeId
  entries: StaffAttendanceUpsert[]
  unitId: DaycareId
}

/**
* Generated from fi.espoo.evaka.attendance.StaffAttendanceResponse
*/
export interface StaffAttendanceResponse {
  extraAttendances: ExternalAttendance[]
  staff: EmployeeAttendance[]
}

/**
* Generated from fi.espoo.evaka.attendance.StaffAttendanceType
*/
export const staffAttendanceTypes = [
  'PRESENT',
  'OTHER_WORK',
  'TRAINING',
  'OVERTIME',
  'JUSTIFIED_CHANGE'
] as const

export type StaffAttendanceType = typeof staffAttendanceTypes[number]

/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.StaffAttendanceUpdateRequest
*/
export interface StaffAttendanceUpdateRequest {
  date: LocalDate
  employeeId: EmployeeId
  pinCode: string
  rows: StaffAttendanceUpsert[]
}

/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.StaffAttendanceUpdateResponse
*/
export interface StaffAttendanceUpdateResponse {
  deleted: StaffAttendanceRealtimeId[]
  inserted: StaffAttendanceRealtimeId[]
}

/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.StaffAttendanceUpsert
*/
export interface StaffAttendanceUpsert {
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
  groupId: GroupId | null
  hasStaffOccupancyEffect: boolean
  id: StaffAttendanceRealtimeId | null
  type: StaffAttendanceType
}

/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.StaffDepartureRequest
*/
export interface StaffDepartureRequest {
  employeeId: EmployeeId
  groupId: GroupId
  pinCode: string
  time: LocalTime
  type: StaffAttendanceType | null
}

/**
* Generated from fi.espoo.evaka.attendance.StaffMember
*/
export interface StaffMember {
  attendances: StaffMemberAttendance[]
  employeeId: EmployeeId
  firstName: string
  groupIds: GroupId[]
  hasFutureAttendances: boolean
  lastName: string
  latestCurrentDayAttendance: StaffMemberAttendance | null
  occupancyEffect: boolean
  plannedAttendances: PlannedStaffAttendance[]
  present: GroupId | null
  spanningPlan: HelsinkiDateTimeRange | null
}

/**
* Generated from fi.espoo.evaka.attendance.StaffMemberAttendance
*/
export interface StaffMemberAttendance {
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
  departedAutomatically: boolean
  employeeId: EmployeeId
  groupId: GroupId | null
  id: StaffAttendanceRealtimeId
  occupancyCoefficient: number
  type: StaffAttendanceType
}

/**
* Generated from fi.espoo.evaka.attendance.UnitInfo
*/
export interface UnitInfo {
  features: PilotFeature[]
  groups: GroupInfo[]
  id: DaycareId
  isOperationalDate: boolean
  name: string
  staff: Staff[]
  utilization: number
}

/**
* Generated from fi.espoo.evaka.attendance.UnitStats
*/
export interface UnitStats {
  id: DaycareId
  name: string
  presentChildren: number
  presentStaff: number
  totalChildren: number
  totalStaff: number
  utilization: number
}


export function deserializeJsonAbsenceRangeRequest(json: JsonOf<AbsenceRangeRequest>): AbsenceRangeRequest {
  return {
    ...json,
    range: FiniteDateRange.parseJson(json.range)
  }
}


export function deserializeJsonArrivalsRequest(json: JsonOf<ArrivalsRequest>): ArrivalsRequest {
  return {
    ...json,
    arrived: LocalTime.parseIso(json.arrived)
  }
}


export function deserializeJsonAttendance(json: JsonOf<Attendance>): Attendance {
  return {
    ...json,
    arrived: HelsinkiDateTime.parseIso(json.arrived),
    arrivedAddedAt: (json.arrivedAddedAt != null) ? HelsinkiDateTime.parseIso(json.arrivedAddedAt) : null,
    arrivedModifiedAt: (json.arrivedModifiedAt != null) ? HelsinkiDateTime.parseIso(json.arrivedModifiedAt) : null,
    departed: (json.departed != null) ? HelsinkiDateTime.parseIso(json.departed) : null,
    departedAddedAt: (json.departedAddedAt != null) ? HelsinkiDateTime.parseIso(json.departedAddedAt) : null,
    departedModifiedAt: (json.departedModifiedAt != null) ? HelsinkiDateTime.parseIso(json.departedModifiedAt) : null
  }
}


export function deserializeJsonAttendanceChild(json: JsonOf<AttendanceChild>): AttendanceChild {
  return {
    ...json,
    dailyNote: (json.dailyNote != null) ? deserializeJsonChildDailyNote(json.dailyNote) : null,
    dailyServiceTimes: (json.dailyServiceTimes != null) ? deserializeJsonDailyServiceTimesValue(json.dailyServiceTimes) : null,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    operationalDates: json.operationalDates.map(e => LocalDate.parseIso(e)),
    reservations: json.reservations.map(e => deserializeJsonReservationResponse(e)),
    stickyNotes: json.stickyNotes.map(e => deserializeJsonChildStickyNote(e))
  }
}


export function deserializeJsonAttendanceTimes(json: JsonOf<AttendanceTimes>): AttendanceTimes {
  return {
    ...json,
    arrived: HelsinkiDateTime.parseIso(json.arrived),
    departed: (json.departed != null) ? HelsinkiDateTime.parseIso(json.departed) : null,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt)
  }
}


export function deserializeJsonChildAttendanceStatusResponse(json: JsonOf<ChildAttendanceStatusResponse>): ChildAttendanceStatusResponse {
  return {
    ...json,
    attendances: json.attendances.map(e => deserializeJsonAttendanceTimes(e))
  }
}


export function deserializeJsonCurrentDayStaffAttendanceResponse(json: JsonOf<CurrentDayStaffAttendanceResponse>): CurrentDayStaffAttendanceResponse {
  return {
    ...json,
    extraAttendances: json.extraAttendances.map(e => deserializeJsonExternalStaffMember(e)),
    staff: json.staff.map(e => deserializeJsonStaffMember(e))
  }
}


export function deserializeJsonDeparturesRequest(json: JsonOf<DeparturesRequest>): DeparturesRequest {
  return {
    ...json,
    departed: LocalTime.parseIso(json.departed)
  }
}


export function deserializeJsonEmployeeAttendance(json: JsonOf<EmployeeAttendance>): EmployeeAttendance {
  return {
    ...json,
    attendances: json.attendances.map(e => deserializeJsonAttendance(e)),
    plannedAttendances: json.plannedAttendances.map(e => deserializeJsonPlannedStaffAttendance(e))
  }
}


export function deserializeJsonExpectedAbsencesOnDeparturesRequest(json: JsonOf<ExpectedAbsencesOnDeparturesRequest>): ExpectedAbsencesOnDeparturesRequest {
  return {
    ...json,
    departed: LocalTime.parseIso(json.departed)
  }
}


export function deserializeJsonExternalAttendance(json: JsonOf<ExternalAttendance>): ExternalAttendance {
  return {
    ...json,
    arrived: HelsinkiDateTime.parseIso(json.arrived),
    departed: (json.departed != null) ? HelsinkiDateTime.parseIso(json.departed) : null
  }
}


export function deserializeJsonExternalAttendanceBody(json: JsonOf<ExternalAttendanceBody>): ExternalAttendanceBody {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    entries: json.entries.map(e => deserializeJsonExternalAttendanceUpsert(e))
  }
}


export function deserializeJsonExternalAttendanceUpsert(json: JsonOf<ExternalAttendanceUpsert>): ExternalAttendanceUpsert {
  return {
    ...json,
    arrived: HelsinkiDateTime.parseIso(json.arrived),
    departed: (json.departed != null) ? HelsinkiDateTime.parseIso(json.departed) : null
  }
}


export function deserializeJsonExternalStaffArrivalRequest(json: JsonOf<ExternalStaffArrivalRequest>): ExternalStaffArrivalRequest {
  return {
    ...json,
    arrived: LocalTime.parseIso(json.arrived)
  }
}


export function deserializeJsonExternalStaffDepartureRequest(json: JsonOf<ExternalStaffDepartureRequest>): ExternalStaffDepartureRequest {
  return {
    ...json,
    time: LocalTime.parseIso(json.time)
  }
}


export function deserializeJsonExternalStaffMember(json: JsonOf<ExternalStaffMember>): ExternalStaffMember {
  return {
    ...json,
    arrived: HelsinkiDateTime.parseIso(json.arrived)
  }
}


export function deserializeJsonOpenGroupAttendance(json: JsonOf<OpenGroupAttendance>): OpenGroupAttendance {
  return {
    ...json,
    date: LocalDate.parseIso(json.date)
  }
}


export function deserializeJsonOpenGroupAttendanceResponse(json: JsonOf<OpenGroupAttendanceResponse>): OpenGroupAttendanceResponse {
  return {
    ...json,
    openGroupAttendance: (json.openGroupAttendance != null) ? deserializeJsonOpenGroupAttendance(json.openGroupAttendance) : null
  }
}


export function deserializeJsonPlannedStaffAttendance(json: JsonOf<PlannedStaffAttendance>): PlannedStaffAttendance {
  return {
    ...json,
    end: HelsinkiDateTime.parseIso(json.end),
    start: HelsinkiDateTime.parseIso(json.start)
  }
}


export function deserializeJsonStaffArrivalRequest(json: JsonOf<StaffArrivalRequest>): StaffArrivalRequest {
  return {
    ...json,
    time: LocalTime.parseIso(json.time)
  }
}


export function deserializeJsonStaffAttendanceBody(json: JsonOf<StaffAttendanceBody>): StaffAttendanceBody {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    entries: json.entries.map(e => deserializeJsonStaffAttendanceUpsert(e))
  }
}


export function deserializeJsonStaffAttendanceResponse(json: JsonOf<StaffAttendanceResponse>): StaffAttendanceResponse {
  return {
    ...json,
    extraAttendances: json.extraAttendances.map(e => deserializeJsonExternalAttendance(e)),
    staff: json.staff.map(e => deserializeJsonEmployeeAttendance(e))
  }
}


export function deserializeJsonStaffAttendanceUpdateRequest(json: JsonOf<StaffAttendanceUpdateRequest>): StaffAttendanceUpdateRequest {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    rows: json.rows.map(e => deserializeJsonStaffAttendanceUpsert(e))
  }
}


export function deserializeJsonStaffAttendanceUpsert(json: JsonOf<StaffAttendanceUpsert>): StaffAttendanceUpsert {
  return {
    ...json,
    arrived: HelsinkiDateTime.parseIso(json.arrived),
    departed: (json.departed != null) ? HelsinkiDateTime.parseIso(json.departed) : null
  }
}


export function deserializeJsonStaffDepartureRequest(json: JsonOf<StaffDepartureRequest>): StaffDepartureRequest {
  return {
    ...json,
    time: LocalTime.parseIso(json.time)
  }
}


export function deserializeJsonStaffMember(json: JsonOf<StaffMember>): StaffMember {
  return {
    ...json,
    attendances: json.attendances.map(e => deserializeJsonStaffMemberAttendance(e)),
    latestCurrentDayAttendance: (json.latestCurrentDayAttendance != null) ? deserializeJsonStaffMemberAttendance(json.latestCurrentDayAttendance) : null,
    plannedAttendances: json.plannedAttendances.map(e => deserializeJsonPlannedStaffAttendance(e)),
    spanningPlan: (json.spanningPlan != null) ? deserializeJsonHelsinkiDateTimeRange(json.spanningPlan) : null
  }
}


export function deserializeJsonStaffMemberAttendance(json: JsonOf<StaffMemberAttendance>): StaffMemberAttendance {
  return {
    ...json,
    arrived: HelsinkiDateTime.parseIso(json.arrived),
    departed: (json.departed != null) ? HelsinkiDateTime.parseIso(json.departed) : null
  }
}
