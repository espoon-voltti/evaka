// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import LocalDate from '../../local-date'
import { AbsenceCareType } from './daycare'
import { AbsenceType } from './daycare'
import { DailyServiceTimes } from '../../api-types/child/common'
import { DaycareDailyNote } from './messaging'
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
  unit: UnitInfo
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
* Generated from fi.espoo.evaka.attendance.Child
*/
export interface Child {
  absences: ChildAbsence[]
  attendance: ChildAttendance | null
  backup: boolean
  dailyNote: DaycareDailyNote | null
  dailyServiceTimes: DailyServiceTimes | null
  firstName: string
  groupId: UUID
  id: UUID
  imageUrl: string | null
  lastName: string
  placementType: PlacementType
  preferredName: string | null
  reservation: AttendanceReservation | null
  status: AttendanceStatus
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAbsence
*/
export interface ChildAbsence {
  careType: AbsenceCareType
  childId: UUID
  id: UUID
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendance
*/
export interface ChildAttendance {
  arrived: Date
  childId: UUID
  departed: Date | null
  id: UUID
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.attendance.ChildResult
*/
export interface ChildResult {
  child: ChildSensitiveInformation | null
  status: ChildResultStatus
}

/**
* Generated from fi.espoo.evaka.attendance.ChildResultStatus
*/
export type ChildResultStatus = 
  | 'SUCCESS'
  | 'WRONG_PIN'
  | 'PIN_LOCKED'
  | 'NOT_FOUND'

/**
* Generated from fi.espoo.evaka.attendance.ChildSensitiveInformation
*/
export interface ChildSensitiveInformation {
  allergies: string
  backupPickups: ContactInfo[]
  childAddress: string
  contacts: ContactInfo[]
  diet: string
  firstName: string
  id: UUID
  lastName: string
  medication: string
  placementTypes: PlacementType[]
  preferredName: string
  ssn: string
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
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.DepartureInfoResponse
*/
export interface DepartureInfoResponse {
  absentFrom: AbsenceCareType[]
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.DepartureRequest
*/
export interface DepartureRequest {
  absenceType: AbsenceType | null
  departed: string
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.FullDayAbsenceRequest
*/
export interface FullDayAbsenceRequest {
  absenceType: AbsenceType
}

/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.GetChildSensitiveInfoRequest
*/
export interface GetChildSensitiveInfoRequest {
  pin: string
  staffId: UUID
}

/**
* Generated from fi.espoo.evaka.attendance.GroupInfo
*/
export interface GroupInfo {
  dailyNote: DaycareDailyNote | null
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
  pinSet: boolean
}

/**
* Generated from fi.espoo.evaka.attendance.UnitInfo
*/
export interface UnitInfo {
  groups: GroupInfo[]
  id: UUID
  name: string
  staff: Staff[]
}
