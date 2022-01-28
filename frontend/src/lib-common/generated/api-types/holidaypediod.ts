// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import FiniteDateRange from '../../finite-date-range'
import LocalDate from '../../local-date'
import { Translatable } from './shared'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.holidaypediod.HolidayPeriod
*/
export interface HolidayPeriod {
  created: Date
  description: Translatable
  descriptionLink: Translatable
  id: UUID
  period: FiniteDateRange
  reservationDeadline: LocalDate
  showReservationBannerFrom: LocalDate
  updated: Date
}

/**
* Generated from fi.espoo.evaka.holidaypediod.HolidayPeriodBody
*/
export interface HolidayPeriodBody {
  description: Translatable
  descriptionLink: Translatable
  period: FiniteDateRange
  reservationDeadline: LocalDate
  showReservationBannerFrom: LocalDate
}
