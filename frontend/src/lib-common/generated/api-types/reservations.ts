// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace */

import type FiniteDateRange from '../../finite-date-range'
import type LocalDate from '../../local-date'
import type LocalTime from '../../local-time'
import { type AbsenceType } from './daycare'
import { type PlacementType } from './placement'
import { type UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.reservations.AbsenceInfo
*/
export interface AbsenceInfo {
  markedByEmployee: boolean
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

export namespace DailyReservationRequest {
  /**
  * Generated from fi.espoo.evaka.reservations.DailyReservationRequest.Absence
  */
  export interface Absence {
    type: 'ABSENCE'
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
  * Generated from fi.espoo.evaka.reservations.DailyReservationRequest.Reservations
  */
  export interface Reservations {
    type: 'RESERVATIONS'
    childId: UUID
    date: LocalDate
    reservation: Reservation
    secondReservation: Reservation | null
  }
}

/**
* Generated from fi.espoo.evaka.reservations.DailyReservationRequest
*/
export type DailyReservationRequest = DailyReservationRequest.Absence | DailyReservationRequest.Nothing | DailyReservationRequest.Reservations


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
  duplicateOf: UUID | null
  firstName: string
  id: UUID
  imageId: UUID | null
  lastName: string
  preferredName: string
  upcomingPlacementType: PlacementType | null
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
  contractDays: boolean
  reservations: Reservation[]
  shiftCare: boolean
}

/**
* Generated from fi.espoo.evaka.reservations.ReservationsResponse
*/
export interface ReservationsResponse {
  children: ReservationChild[]
  days: ReservationResponseDay[]
  includesWeekends: boolean
  reservableRange: FiniteDateRange
}
