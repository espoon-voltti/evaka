// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from '../local-date'

export interface DailyReservationRequest {
  childId: string
  date: LocalDate
  reservations: [TimeRange] | [TimeRange, TimeRange] | null
}

export interface TimeRange {
  startTime: string
  endTime: string
}
