// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from './date-range'
import FiniteDateRange from './finite-date-range'
import HelsinkiDateTime from './helsinki-date-time'
import LocalDate from './local-date'
import LocalTime from './local-time'
import TimeInterval from './time-interval'
import TimeRange from './time-range'

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
              : T extends TimeInterval
                ? { start: JsonOf<LocalTime>; end: JsonOf<LocalTime> | null }
                : T extends TimeRange
                  ? { start: JsonOf<LocalTime>; end: JsonOf<LocalTime> }
                  : T extends Map<string, infer U>
                    ? { [key: string]: JsonOf<U> }
                    : T extends Set<infer U>
                      ? JsonOf<U>[]
                      : T extends [infer A, infer B]
                        ? [JsonOf<A>, JsonOf<B>]
                        : T extends [infer A, infer B, infer C]
                          ? [JsonOf<A>, JsonOf<B>, JsonOf<C>]
                          : T extends (infer U)[]
                            ? JsonOf<U>[]
                            : T extends object // eslint-disable-line @typescript-eslint/ban-types
                              ? { [P in keyof T]: JsonOf<T[P]> }
                              : never

/**
 * Type operator to check if the given type can be converted to reasonable JSON without extra code.
 *
 * Use with the TS satisfies operator: `someValue satisfies JsonCompatible<TypeOfThatValue>`
 */
export type JsonCompatible<T> = T extends
  | string
  | number
  | boolean
  | null
  | undefined
  | { toJSON(): string }
  ? T
  : T extends (infer U)[]
    ? JsonCompatible<U>[]
    : T extends object // eslint-disable-line @typescript-eslint/ban-types
      ? { [P in keyof T]: JsonCompatible<T[P]> }
      : never
