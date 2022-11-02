// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import LocalTime from '../../local-time'
import { AbsenceCategory } from './daycare'
import { AbsenceType } from './daycare'
import { ChildDailyNote } from './note'
import { ChildStickyNote } from './note'
import { DailyServiceTimesValue } from '../../api-types/child/common'
import { GroupNote } from './note'
import { HelsinkiDateTimeRange } from './shared'
import { PilotFeature } from './shared'
import { PlacementType } from './placement'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.AbsenceRangeRequest
*/
export interface AbsenceRangeRequest {
  absenceType: AbsenceType
  endDate: LocalDate
  startDate: LocalDate
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
  groupId: UUID
  id: UUID
  occupancyCoefficient: number
  type: StaffAttendanceType
}

/**
* Generated from fi.espoo.evaka.attendance.AttendanceReservation
*/
export interface AttendanceReservation {
  endTime: HelsinkiDateTime
  startTime: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.attendance.AttendanceResponse
*/
export interface AttendanceResponse {
  children: Child[]
  groupNotes: GroupNote[]
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
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.AttendanceTimeRange
*/
export interface AttendanceTimeRange {
  endTime: string | null
  startTime: string
}

/**
* Generated from fi.espoo.evaka.attendance.AttendanceTimes
*/
export interface AttendanceTimes {
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.AttendancesRequest
*/
export interface AttendancesRequest {
  attendances: AttendanceTimeRange[]
  date: LocalDate
}

/**
* Generated from fi.espoo.evaka.attendance.Child
*/
export interface Child {
  absences: ChildAbsence[]
  attendance: AttendanceTimes | null
  backup: boolean
  dailyNote: ChildDailyNote | null
  dailyServiceTimes: DailyServiceTimesValue | null
  firstName: string
  groupId: UUID | null
  id: UUID
  imageUrl: string | null
  lastName: string
  placementType: PlacementType
  preferredName: string | null
  reservations: AttendanceReservation[]
  status: AttendanceStatus
  stickyNotes: ChildStickyNote[]
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAbsence
*/
export interface ChildAbsence {
  category: AbsenceCategory
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
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.ExternalStaffArrivalRequest
*/
export interface ExternalStaffArrivalRequest {
  arrived: LocalTime
  groupId: UUID
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
* Generated from fi.espoo.evaka.attendance.OccupancyCoefficientUpsert
*/
export interface OccupancyCoefficientUpsert {
  coefficient: number
  employeeId: UUID
  unitId: UUID
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
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.SingleDayStaffAttendanceUpsert
*/
export interface SingleDayStaffAttendanceUpsert {
  arrived: HelsinkiDateTime
  attendanceId: UUID | null
  departed: HelsinkiDateTime | null
  groupId: UUID
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
  groupId: UUID
  id: UUID
  type: StaffAttendanceType
}

/**
* Generated from fi.espoo.evaka.attendance.StaffOccupancyCoefficient
*/
export interface StaffOccupancyCoefficient {
  coefficient: number
  employeeId: UUID
  firstName: string
  id: UUID
  lastName: string
}

/**
* Generated from fi.espoo.evaka.attendance.UnitInfo
*/
export interface UnitInfo {
  features: PilotFeature[]
  groups: GroupInfo[]
  id: UUID
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
  groupId: UUID
  type: StaffAttendanceType
}
