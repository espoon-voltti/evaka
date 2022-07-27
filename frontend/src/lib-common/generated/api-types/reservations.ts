// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import type FiniteDateRange from '../../finite-date-range'
import type LocalDate from '../../local-date'
import type { AbsenceType } from './daycare'
import type { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.reservations.AbsenceRequest
*/
export interface AbsenceRequest {
  absenceType: AbsenceType
  childIds: UUID[]
  dateRange: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.reservations.ChildDailyData
*/
export interface ChildDailyData {
  absence: AbsenceType | null
  attendances: OpenTimeRange[]
  childId: UUID
  dayOff: boolean
  markedByEmployee: boolean
  reservations: TimeRange[]
}

/**
* Generated from fi.espoo.evaka.reservations.DailyReservationData
*/
export interface DailyReservationData {
  children: ChildDailyData[]
  date: LocalDate
  isHoliday: boolean
}

/**
* Generated from fi.espoo.evaka.reservations.DailyReservationRequest
*/
export interface DailyReservationRequest {
  absent: boolean
  childId: UUID
  date: LocalDate
  reservations: TimeRange[] | null
}

/**
* Generated from fi.espoo.evaka.reservations.OpenTimeRange
*/
export interface OpenTimeRange {
  endTime: string | null
  startTime: string
}

/**
* Generated from fi.espoo.evaka.reservations.ReservationChild
*/
export interface ReservationChild {
  firstName: string
  id: UUID
  imageId: UUID | null
  inShiftCareUnit: boolean
  maxOperationalDays: number[]
  placementMaxEnd: LocalDate
  placementMinStart: LocalDate
  preferredName: string | null
}

/**
* Generated from fi.espoo.evaka.reservations.ReservationsResponse
*/
export interface ReservationsResponse {
  children: ReservationChild[]
  dailyData: DailyReservationData[]
  includesWeekends: boolean
  reservableDays: FiniteDateRange[]
}

/**
* Generated from fi.espoo.evaka.reservations.TimeRange
*/
export interface TimeRange {
  endTime: string
  startTime: string
}
