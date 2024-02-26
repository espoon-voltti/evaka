// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

export interface PeriodFilters {
  from: LocalDate
  to: LocalDate
}

export interface DateFilters {
  date: LocalDate
}
