// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace */

import FiniteDateRange from '../../finite-date-range'
import LocalDate from '../../local-date'
import LocalTime from '../../local-time'
import { AbsenceType } from './daycare'
import { UUID } from '../../types'

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
  endTime: LocalTime | null
  startTime: LocalTime
}

/**
* Generated from fi.espoo.evaka.reservations.ReservationChild
*/
export interface ReservationChild {
  firstName: string
  hasContractDays: boolean
  id: UUID
  imageId: UUID | null
  inShiftCareUnit: boolean
  lastName: string
  maxOperationalDays: number[]
  placements: FiniteDateRange[]
  preferredName: string
}

/**
* Generated from fi.espoo.evaka.reservations.ReservationsResponse
*/
export interface ReservationsResponse {
  children: ReservationChild[]
  dailyData: DailyReservationData[]
  includesWeekends: boolean
  reservableDays: Record<string, FiniteDateRange[]>
}

/**
* Generated from fi.espoo.evaka.reservations.TimeRange
*/
export interface TimeRange {
  endTime: LocalTime
  startTime: LocalTime
}
