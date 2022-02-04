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
* Generated from fi.espoo.evaka.holidayperiod.FreeAbsencePeriod
*/
export interface FreeAbsencePeriod {
  deadline: LocalDate
  periodOptionLabel: Translatable
  periodOptions: FiniteDateRange[]
  questionLabel: Translatable
}

/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriod
*/
export interface HolidayPeriod {
  description: Translatable
  descriptionLink: Translatable
  freePeriod: FreeAbsencePeriod | null
  id: UUID
  period: FiniteDateRange
  reservationDeadline: LocalDate
  showReservationBannerFrom: LocalDate
}

/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodBody
*/
export interface HolidayPeriodBody {
  description: Translatable
  descriptionLink: Translatable
  freePeriod: FreeAbsencePeriod | null
  period: FiniteDateRange
  reservationDeadline: LocalDate
  showReservationBannerFrom: LocalDate
}
