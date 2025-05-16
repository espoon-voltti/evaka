// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type LocalDate from 'lib-common/local-date'

export interface PeriodFilters {
  from: LocalDate | null
  to: LocalDate | null
}

export interface DateFilters {
  date: LocalDate | null
}
