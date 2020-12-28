// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from './local-date'
import FiniteDateRange from './finite-date-range'
import DateRange from './date-range'

// eslint-disable-next-line @typescript-eslint/ban-types
type JsonOfObject<T extends object> = { [P in keyof T]: JsonOf<T[P]> }
export type JsonOf<T> = T extends string | number | boolean | null | undefined
  ? T
  : T extends Date
  ? string
  : T extends LocalDate
  ? string
  : T extends FiniteDateRange
  ? { start: JsonOf<LocalDate>; end: JsonOf<LocalDate> }
  : T extends DateRange
  ? { start: JsonOf<LocalDate>; end: JsonOf<LocalDate> | null }
  : T extends Set<infer U>
  ? Array<JsonOf<U>>
  : T extends Array<infer U>
  ? Array<JsonOf<U>>
  : T extends object // eslint-disable-line @typescript-eslint/ban-types
  ? JsonOfObject<T>
  : never
