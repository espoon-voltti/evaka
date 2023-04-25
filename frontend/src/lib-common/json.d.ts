// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type DateRange from './date-range'
import type FiniteDateRange from './finite-date-range'
import type HelsinkiDateTime from './helsinki-date-time'
import type LocalDate from './local-date'
import type LocalTime from './local-time'

export type JsonOf<T> = T extends string | number | boolean | null | undefined
  ? T
  : T extends Date
  ? string
  : T extends LocalDate
  ? string
  : T extends LocalTime
  ? string
  : T extends HelsinkiDateTime
  ? string
  : T extends FiniteDateRange
  ? { start: JsonOf<LocalDate>; end: JsonOf<LocalDate> }
  : T extends DateRange
  ? { start: JsonOf<LocalDate>; end: JsonOf<LocalDate> | null }
  : T extends Map<string, infer U>
  ? { [key: string]: JsonOf<U> }
  : T extends Set<infer U>
  ? Array<JsonOf<U>>
  : T extends Array<infer U>
  ? Array<JsonOf<U>>
  : T extends object // eslint-disable-line @typescript-eslint/ban-types
  ? { [P in keyof T]: JsonOf<T[P]> }
  : never
