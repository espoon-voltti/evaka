// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import LocalTime from '../../local-time'
import { AbsenceCategory } from './daycare'
import { AbsenceType } from './daycare'
import { ChildDailyNote } from './note'
import { ChildStickyNote } from './note'
import { DailyServiceTimesValue } from './dailyservicetimes'
import { HelsinkiDateTimeRange } from './shared'
import { PilotFeature } from './shared'
import { PlacementType } from './placement'
import { Reservation } from './reservations'
import { ScheduleType } from './placement'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.AbsenceRangeRequest
*/
export interface AbsenceRangeRequest {
  absenceType: AbsenceType
  range: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.attendance.AbsenceThreshold
*/
export interface AbsenceThreshold {
  category: AbsenceCategory
  time: string
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.ArrivalRequest
*/
export interface ArrivalRequest {
  arrived: string
}

/**
* Generated from fi.espoo.evaka.attendance.Attendance
*/
export interface Attendance {
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
  groupId: UUID | null
  id: UUID
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
  groupId: UUID | null
  id: UUID
  imageUrl: string | null
  lastName: string
  placementType: PlacementType
  preferredName: string
  reservations: Reservation[]
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
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAbsence
*/
export interface ChildAbsence {
  category: AbsenceCategory
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
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.DepartureRequest
*/
export interface DepartureRequest {
  absenceType: AbsenceType | null
  departed: string
}

/**
* Generated from fi.espoo.evaka.attendance.EmployeeAttendance
*/
export interface EmployeeAttendance {
  attendances: Attendance[]
  currentOccupancyCoefficient: number
  employeeId: UUID
  firstName: string
  groups: UUID[]
  lastName: string
  plannedAttendances: PlannedStaffAttendance[]
}

/**
* Generated from fi.espoo.evaka.attendance.ExternalAttendance
*/
export interface ExternalAttendance {
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
  groupId: UUID
  id: UUID
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
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.ExternalAttendanceUpsert
*/
export interface ExternalAttendanceUpsert {
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
  groupId: UUID
  hasStaffOccupancyEffect: boolean
  id: UUID | null
}

/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.ExternalStaffArrivalRequest
*/
export interface ExternalStaffArrivalRequest {
  arrived: LocalTime
  groupId: UUID
  hasStaffOccupancyEffect: boolean
  name: string
}

/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.ExternalStaffDepartureRequest
*/
export interface ExternalStaffDepartureRequest {
  attendanceId: UUID
  time: LocalTime
}

/**
* Generated from fi.espoo.evaka.attendance.ExternalStaffMember
*/
export interface ExternalStaffMember {
  arrived: HelsinkiDateTime
  groupId: UUID
  id: UUID
  name: string
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
  id: UUID
  name: string
  utilization: number
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
  groups: UUID[]
  id: UUID
  lastName: string
  pinLocked: boolean
  pinSet: boolean
}

/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.StaffArrivalRequest
*/
export interface StaffArrivalRequest {
  employeeId: UUID
  groupId: UUID
  pinCode: string
  time: LocalTime
  type: StaffAttendanceType | null
}

/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.StaffAttendanceBody
*/
export interface StaffAttendanceBody {
  date: LocalDate
  employeeId: UUID
  entries: StaffAttendanceUpsert[]
  unitId: UUID
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
  employeeId: UUID
  pinCode: string
  rows: StaffAttendanceUpsert[]
}

/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.StaffAttendanceUpdateResponse
*/
export interface StaffAttendanceUpdateResponse {
  deleted: UUID[]
  inserted: UUID[]
}

/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.StaffAttendanceUpsert
*/
export interface StaffAttendanceUpsert {
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
  groupId: UUID | null
  id: UUID | null
  type: StaffAttendanceType
}

/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.StaffDepartureRequest
*/
export interface StaffDepartureRequest {
  employeeId: UUID
  groupId: UUID
  pinCode: string
  time: LocalTime
  type: StaffAttendanceType | null
}

/**
* Generated from fi.espoo.evaka.attendance.StaffMember
*/
export interface StaffMember {
  attendances: StaffMemberAttendance[]
  employeeId: UUID
  firstName: string
  groupIds: UUID[]
  hasFutureAttendances: boolean
  lastName: string
  latestCurrentDayAttendance: StaffMemberAttendance | null
  plannedAttendances: PlannedStaffAttendance[]
  present: UUID | null
  spanningPlan: HelsinkiDateTimeRange | null
}

/**
* Generated from fi.espoo.evaka.attendance.StaffMemberAttendance
*/
export interface StaffMemberAttendance {
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
  employeeId: UUID
  groupId: UUID | null
  id: UUID
  type: StaffAttendanceType
}

/**
* Generated from fi.espoo.evaka.attendance.UnitInfo
*/
export interface UnitInfo {
  features: PilotFeature[]
  groups: GroupInfo[]
  id: UUID
  isOperationalDate: boolean
  name: string
  staff: Staff[]
  utilization: number
}

/**
* Generated from fi.espoo.evaka.attendance.UnitStats
*/
export interface UnitStats {
  id: UUID
  name: string
  presentChildren: number
  presentStaff: number
  presentStaffOther: number
  totalChildren: number
  totalStaff: number
  utilization: number
}

/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.UpsertExternalAttendance
*/
export interface UpsertExternalAttendance {
  arrived: HelsinkiDateTime
  attendanceId: UUID | null
  departed: HelsinkiDateTime | null
  groupId: UUID
  hasStaffOccupancyEffect: boolean
  name: string | null
}

/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.UpsertStaffAndExternalAttendanceRequest
*/
export interface UpsertStaffAndExternalAttendanceRequest {
  externalAttendances: UpsertExternalAttendance[]
  staffAttendances: UpsertStaffAttendance[]
}

/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.UpsertStaffAttendance
*/
export interface UpsertStaffAttendance {
  arrived: HelsinkiDateTime
  attendanceId: UUID | null
  departed: HelsinkiDateTime | null
  employeeId: UUID
  groupId: UUID | null
  type: StaffAttendanceType
}
