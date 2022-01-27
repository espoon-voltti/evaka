// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import LocalDate from '../../local-date'
import { AbsenceCategory } from './daycare'
import { AbsenceType } from './daycare'
import { ChildDailyNote } from './note'
import { ChildStickyNote } from './note'
import { DailyServiceTimes } from '../../api-types/child/common'
import { GroupNote } from './note'
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
* Generated from fi.espoo.evaka.attendance.AttendanceReservation
*/
export interface AttendanceReservation {
  endTime: string
  startTime: string
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
* Generated from fi.espoo.evaka.attendance.AttendanceTimes
*/
export interface AttendanceTimes {
  arrived: Date
  departed: Date | null
}

/**
* Generated from fi.espoo.evaka.attendance.Child
*/
export interface Child {
  absences: ChildAbsence[]
  attendance: AttendanceTimes | null
  backup: boolean
  dailyNote: ChildDailyNote | null
  dailyServiceTimes: DailyServiceTimes | null
  firstName: string
  groupId: UUID
  id: UUID
  imageUrl: string | null
  lastName: string
  placementType: PlacementType
  preferredName: string | null
  reservation: AttendanceReservation | null
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
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.DepartureRequest
*/
export interface DepartureRequest {
  absenceType: AbsenceType | null
  departed: string
}

/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.ExternalStaffArrivalRequest
*/
export interface ExternalStaffArrivalRequest {
  arrived: string
  groupId: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.ExternalStaffDepartureRequest
*/
export interface ExternalStaffDepartureRequest {
  attendanceId: UUID
  time: string
}

/**
* Generated from fi.espoo.evaka.attendance.ExternalStaffMember
*/
export interface ExternalStaffMember {
  arrived: Date
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
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.StaffArrivalRequest
*/
export interface StaffArrivalRequest {
  employeeId: UUID
  groupId: UUID
  pinCode: string
  time: string
}

/**
* Generated from fi.espoo.evaka.attendance.StaffAttendanceResponse
*/
export interface StaffAttendanceResponse {
  extraAttendances: ExternalStaffMember[]
  staff: StaffMember[]
}

/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.StaffDepartureRequest
*/
export interface StaffDepartureRequest {
  pinCode: string
  time: string
}

/**
* Generated from fi.espoo.evaka.attendance.StaffMember
*/
export interface StaffMember {
  employeeId: UUID
  firstName: string
  groupIds: UUID[]
  lastName: string
  latestCurrentDayAttendance: StaffMemberAttendance | null
  present: UUID | null
}

/**
* Generated from fi.espoo.evaka.attendance.StaffMemberAttendance
*/
export interface StaffMemberAttendance {
  arrived: Date
  departed: Date | null
  employeeId: UUID
  groupId: UUID
  id: UUID
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
}

/**
* Generated from fi.espoo.evaka.attendance.UnitStats
*/
export interface UnitStats {
  id: UUID
  name: string
  presentChildren: number
  presentStaff: number
  totalChildren: number
  totalStaff: number
}
