// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace */

import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
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
  reservations: Reservation[]
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
  reservations: Reservation[] | null
}

/**
* Generated from fi.espoo.evaka.reservations.OpenTimeRange
*/
export interface OpenTimeRange {
  endTime: LocalTime | null
  startTime: LocalTime
}

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

export namespace ReservationSpan {
  /**
  * Generated from fi.espoo.evaka.reservations.ReservationSpan.NoTimes
  */
  export interface NoTimes {
    type: 'NO_TIMES'
    date: LocalDate
  }
  
  /**
  * Generated from fi.espoo.evaka.reservations.ReservationSpan.Times
  */
  export interface Times {
    type: 'TIMES'
    endTime: HelsinkiDateTime
    startTime: HelsinkiDateTime
  }
}

/**
* Generated from fi.espoo.evaka.reservations.ReservationSpan
*/
export type ReservationSpan = ReservationSpan.NoTimes | ReservationSpan.Times


/**
* Generated from fi.espoo.evaka.reservations.ReservationsResponse
*/
export interface ReservationsResponse {
  children: ReservationChild[]
  dailyData: DailyReservationData[]
  includesWeekends: boolean
  reservableDays: Record<string, FiniteDateRange[]>
}
