// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import uniqBy from 'lodash/uniqBy'

import FiniteDateRange from 'lib-common/finite-date-range'
import {
  string,
  localTimeRange,
  localDateRange,
  FieldType
} from 'lib-common/form/fields'
import {
  array,
  mapped,
  object,
  oneOf,
  required,
  transformed,
  union,
  validated,
  value
} from 'lib-common/form/form'
import {
  FieldErrors,
  StateOf,
  ValidationError,
  ValidationResult,
  ValidationSuccess
} from 'lib-common/form/types'
import {
  DailyReservationRequest,
  ReservationChild,
  ReservationResponseDay
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { Repetition } from 'lib-common/reservations'
import TimeRange from 'lib-common/time-range'
import { UUID } from 'lib-common/types'
import { Translations } from 'lib-customizations/citizen'

export const MAX_TIME_RANGE = new TimeRange(LocalTime.MIN, LocalTime.MAX)

export const limitedLocalTimeRange = () =>
  transformed(
    object({
      value: localTimeRange(),
      validRange: value<TimeRange>()
    }),
    ({
      value,
      validRange
    }): ValidationResult<TimeRange | undefined, 'timeFormat' | 'range'> => {
      if (value === undefined) return ValidationSuccess.of(undefined)

      // Don't allow reservations with same start and end times
      if (value.start.isEqual(value.end)) {
        return ValidationError.field('value', 'timeFormat')
      }

      let errors: FieldErrors<'range'> | undefined = undefined
      if (!timeRangeContains(value.start, validRange)) {
        errors = errors ?? {}
        errors.startTime = 'range'
      }
      if (!timeRangeContains(value.end, validRange)) {
        errors = errors ?? {}
        errors.endTime = 'range'
      }
      if (errors !== undefined) {
        return ValidationError.fromFieldErrors({ value: errors })
      } else {
        return ValidationSuccess.of(value)
      }
    }
  )

export type LimitedLocalTimeRangeField = FieldType<typeof limitedLocalTimeRange>

export function timeRangeContains(
  inputTime: LocalTime,
  { start, end }: TimeRange
) {
  return inputTime.isEqualOrAfter(start) && inputTime.isEqualOrBefore(end)
}

export function emptyTimeRange(
  validRange: TimeRange
): StateOf<LimitedLocalTimeRangeField> {
  return {
    value: { startTime: '', endTime: '' },
    validRange
  }
}

export const timeRanges = mapped(array(limitedLocalTimeRange()), (output) => {
  const nonEmpty = output.flatMap((x) => x ?? [])
  return nonEmpty.length === 0
    ? undefined // All inputs empty => no value
    : nonEmpty
})

export const reservation = object({
  reservation: union({
    timeRanges,
    absent: value<true>()
  }),
  // `timeRanges` needs the valid time range, so it needs to be saved here
  // because `absent` can be changed to `timeRanges`
  validTimeRange: value<TimeRange>()
})

export const noTimes = value<'present' | 'absent' | 'notSet'>()

type DayOutput =
  | { type: 'readOnly' }
  | { type: 'reservations'; first: TimeRange; second: TimeRange | undefined }
  | { type: 'present' }
  | { type: 'absent' }
  | { type: 'nothing' }
  | undefined

export const day = mapped(
  union({
    readOnly: value<
      | 'noChildren'
      | 'holiday'
      | 'absentNotEditable'
      | 'termBreak'
      | 'reservationClosed'
    >(),
    reservation,
    reservationNoTimes: noTimes
  }),
  ({ branch, value }): DayOutput => {
    switch (branch) {
      case 'readOnly':
        return { type: 'readOnly' }
      case 'reservation': {
        switch (value.reservation.branch) {
          case 'timeRanges':
            return value.reservation.value === undefined
              ? { type: 'nothing' }
              : {
                  type: 'reservations',
                  first: value.reservation.value[0],
                  second: value.reservation.value[1]
                }
          case 'absent':
            return { type: 'absent' }
        }
        break
      }
      case 'reservationNoTimes':
        switch (value) {
          case 'present':
            return { type: 'present' }
          case 'absent':
            return { type: 'absent' }
          case 'notSet':
            return { type: 'nothing' }
        }
    }
  }
)

export const dailyTimes = object({
  weekDayRange: value<[number, number]>(),
  day
})

export const weekDay = object({
  weekDay: value<number>(),
  day
})

export const weeklyTimes = array(weekDay)

export const irregularDay = object({
  date: value<LocalDate>(),
  day
})

export const irregularTimes = array(irregularDay)

export const timesUnion = transformed(
  union({
    dailyTimes,
    weeklyTimes,
    irregularTimes,
    notInitialized: value<undefined>()
  }),
  (value) =>
    value.branch === 'notInitialized'
      ? ValidationError.of('required')
      : ValidationSuccess.of(value)
)

export function toDailyReservationRequest(
  childId: UUID,
  date: LocalDate,
  day: DayOutput
): DailyReservationRequest | undefined {
  if (day === undefined) {
    return { type: 'NOTHING', childId, date }
  }
  switch (day.type) {
    case 'readOnly':
      return undefined
    case 'reservations':
      return {
        type: 'RESERVATIONS',
        childId,
        date,
        reservation: day.first,
        secondReservation: day.second ?? null
      }
    case 'present':
      return { type: 'PRESENT', childId, date }
    case 'absent':
      return { type: 'ABSENT', childId, date }
    case 'nothing':
      return { type: 'NOTHING', childId, date }
  }
}

export const reservationForm = mapped(
  object({
    selectedChildren: validated(array(string()), (value) =>
      value.length > 0 ? undefined : 'required'
    ),
    dateRange: required(localDateRange()),
    repetition: required(oneOf<Repetition>()),
    times: timesUnion
  }),
  (output) => ({
    toRequest: (dayProperties: DayProperties): DailyReservationRequest[] =>
      output.selectedChildren.flatMap((childId): DailyReservationRequest[] => {
        const dates = dayProperties.getReservableDatesInRangeForChild(
          output.dateRange,
          childId
        )
        switch (output.times.branch) {
          case 'dailyTimes': {
            const reservations = output.times.value.day
            return dates.flatMap(
              (date) =>
                toDailyReservationRequest(childId, date, reservations) ?? []
            )
          }
          case 'weeklyTimes': {
            const timesValue = output.times.value
            return dates.flatMap((date) => {
              const index = timesValue.findIndex(
                (x) => x.weekDay === date.getIsoDayOfWeek()
              )
              return index === -1
                ? []
                : toDailyReservationRequest(
                    childId,
                    date,
                    timesValue[index].day
                  ) ?? []
            })
          }
          case 'irregularTimes':
            return output.times.value
              .filter((irregularDay) =>
                output.dateRange.includes(irregularDay.date)
              )
              .flatMap(
                ({ date, day }) =>
                  toDailyReservationRequest(childId, date, day) ?? []
              )
        }
      })
  })
)

function repetitionOptions(i18n: Translations) {
  return [
    {
      value: 'DAILY' as const,
      domValue: 'DAILY',
      label: i18n.calendar.reservationModal.repetitions.DAILY
    },
    {
      value: 'WEEKLY' as const,
      domValue: 'WEEKLY',
      label: i18n.calendar.reservationModal.repetitions.WEEKLY
    },
    {
      value: 'IRREGULAR' as const,
      domValue: 'IRREGULAR',
      label: i18n.calendar.reservationModal.repetitions.IRREGULAR
    }
  ]
}

export interface HolidayPeriodInfo {
  period: FiniteDateRange
  state: 'open' | 'closed'
}

export function initialState(
  dayProperties: DayProperties,
  availableChildren: ReservationChild[],
  initialStart: LocalDate | null,
  initialEnd: LocalDate | null,
  i18n: Translations
): StateOf<typeof reservationForm> {
  const selectedChildren = availableChildren.map((child) => child.id)
  return {
    selectedChildren,
    dateRange: localDateRange.fromDates(initialStart, initialEnd, {
      minDate: dayProperties.minDate,
      maxDate: dayProperties.maxDate
    }),
    repetition: {
      domValue:
        initialStart !== null && initialEnd !== null
          ? ('IRREGULAR' as const)
          : ('DAILY' as const),
      options: repetitionOptions(i18n)
    },
    times:
      initialStart !== null && initialEnd !== null
        ? resetTimes(dayProperties, undefined, {
            repetition: 'IRREGULAR',
            selectedRange: new FiniteDateRange(initialStart, initialEnd),
            selectedChildren
          })
        : { branch: 'notInitialized', state: undefined }
  }
}

export function resetTimes(
  dayProperties: DayProperties,
  prev:
    | {
        childrenChanged: boolean
        times: StateOf<typeof timesUnion>
      }
    | undefined,
  next: {
    repetition: Repetition
    selectedRange: FiniteDateRange
    selectedChildren: UUID[]
  }
): StateOf<typeof timesUnion> {
  const { repetition, selectedRange, selectedChildren } = next

  const calendarDaysInRange = dayProperties.calendarDays.filter((day) =>
    selectedRange.includes(day.date)
  )
  const selectedRangeDates = [...selectedRange.dates()]
  const includedWeekDays = [1, 2, 3, 4, 5, 6, 7].filter(
    (dayOfWeek) =>
      dayProperties.isOperationalDayForAnyChild(dayOfWeek) &&
      selectedRangeDates.some((date) => date.getIsoDayOfWeek() === dayOfWeek)
  )
  if (repetition === 'DAILY') {
    if (includedWeekDays.length === 0) {
      // This doesn't happen in practice because selectedRange is limited
      // so that at least one child has a placement
      return {
        branch: 'notInitialized',
        state: undefined
      }
    }

    const holidayPeriodState =
      dayProperties.holidayPeriodStateForRange(selectedRange)

    return {
      branch: 'dailyTimes',
      state: {
        weekDayRange: [
          includedWeekDays[0],
          includedWeekDays[includedWeekDays.length - 1]
        ],
        day: resetDay(holidayPeriodState, calendarDaysInRange, selectedChildren)
      }
    }
  } else if (repetition === 'WEEKLY') {
    const groupedDays = groupBy(selectedRangeDates, (date) =>
      date.getIsoDayOfWeek()
    )

    const holidayPeriodState =
      dayProperties.holidayPeriodStateForRange(selectedRange)

    const weeklyTimes = includedWeekDays.map(
      (dayOfWeek): StateOf<typeof weekDay> => {
        const dayOfWeekDays = groupedDays[dayOfWeek]
        if (!dayOfWeekDays) {
          return {
            weekDay: dayOfWeek,
            day: { branch: 'readOnly', state: 'noChildren' }
          }
        }

        const relevantCalendarDays = calendarDaysInRange.filter(({ date }) =>
          dayOfWeekDays.some((d) => d.isEqual(date))
        )

        return {
          weekDay: dayOfWeek,
          day: resetDay(
            holidayPeriodState,
            relevantCalendarDays,
            selectedChildren
          )
        }
      }
    )
    return {
      branch: 'weeklyTimes',
      state: weeklyTimes
    }
  } else if (repetition === 'IRREGULAR') {
    // Attempt to reuse previous state if selected children are the same
    const prevState =
      prev && !prev.childrenChanged && prev.times.branch === 'irregularTimes'
        ? prev.times.state
        : undefined

    const irregularTimes = calendarDaysInRange.map(
      (calendarDay): StateOf<typeof irregularDay> => {
        const rangeDate = calendarDay.date

        if (prevState) {
          const existing = prevState.find((day) => day.date.isEqual(rangeDate))
          if (existing) return existing
        }

        return {
          date: rangeDate,
          day: resetDay(
            dayProperties.holidayPeriodStateForDate(rangeDate),
            [calendarDay],
            selectedChildren
          )
        }
      }
    )
    return {
      branch: 'irregularTimes',
      state: irregularTimes
    }
  } else {
    throw new Error('Not reached')
  }
}

export function resetDay(
  holidayPeriodState: 'open' | 'closed' | undefined,
  calendarDays: ReservationResponseDay[],
  selectedChildren: UUID[]
): StateOf<typeof day> {
  const reservationNotRequired = reservationNotRequiredForAnyChild(
    calendarDays,
    selectedChildren
  )

  const dayChildren = calendarDays.flatMap((day) =>
    day.children.filter((dayChild) =>
      selectedChildren.includes(dayChild.childId)
    )
  )

  if (holidayWithNoChildrenInShiftCare(calendarDays, selectedChildren)) {
    return { branch: 'readOnly', state: 'holiday' }
  }

  if (dayChildren.length === 0) {
    return {
      branch: 'readOnly',
      state: 'noChildren'
    }
  }

  if (allChildrenAreAbsentNotEditable(calendarDays, selectedChildren)) {
    return {
      branch: 'readOnly',
      state: 'absentNotEditable'
    }
  }

  if (allChildrenOnTermBreak(calendarDays, selectedChildren)) {
    return {
      branch: 'readOnly',
      state: 'termBreak'
    }
  }

  const validTimeRanges = calendarDays.flatMap((d) =>
    d.children
      .filter((c) => selectedChildren.includes(c.childId))
      .map((c) =>
        // Any times can be reserved for intermittent shift care children
        c.reservableTimeRange.type === 'INTERMITTENT_SHIFT_CARE'
          ? MAX_TIME_RANGE
          : c.reservableTimeRange.range
      )
  )
  const validTimeRange = timeRangeIntersection(validTimeRanges)

  if (allChildrenAreAbsent(calendarDays, selectedChildren)) {
    return holidayPeriodState === 'open' ||
      (holidayPeriodState === undefined && reservationNotRequired)
      ? { branch: 'reservationNoTimes', state: 'absent' }
      : holidayPeriodState === 'closed'
        ? { branch: 'readOnly', state: 'reservationClosed' }
        : {
            branch: 'reservation',
            state: {
              validTimeRange,
              reservation: { branch: 'absent', state: true }
            }
          }
  }

  if (hasReservationsForEveryChild(calendarDays, selectedChildren)) {
    if (holidayPeriodState === 'open' || reservationNotRequired) {
      return { branch: 'reservationNoTimes', state: 'present' }
    }

    const commonTimeRanges = getCommonTimeRanges(calendarDays, selectedChildren)
    if (commonTimeRanges) {
      return {
        branch: 'reservation',
        state: {
          validTimeRange,
          reservation: {
            branch: 'timeRanges',
            state: bindUnboundedTimeRanges(commonTimeRanges, validTimeRange)
          }
        }
      }
    }
  }

  if (
    holidayPeriodState === 'closed' &&
    hasNoReservationsForSomeChild(calendarDays, selectedChildren)
  ) {
    return { branch: 'readOnly', state: 'reservationClosed' }
  }

  return holidayPeriodState === 'open' || reservationNotRequired
    ? { branch: 'reservationNoTimes', state: 'notSet' }
    : {
        branch: 'reservation',
        state: {
          validTimeRange,
          reservation: {
            branch: 'timeRanges',
            state: [emptyTimeRange(validTimeRange)]
          }
        }
      }
}

const holidayWithNoChildrenInShiftCare = (
  calendarDays: ReservationResponseDay[],
  selectedChildren: string[]
) =>
  calendarDays.every(
    (day) =>
      day.holiday &&
      day.children.every(
        (child) => !child.shiftCare || !selectedChildren.includes(child.childId)
      )
  )

const hasReservationsForEveryChild = (
  calendarDays: ReservationResponseDay[],
  selectedChildren: string[]
) =>
  calendarDays.every((day) =>
    selectedChildren.every((childId) =>
      day.children.some(
        (child) =>
          child.childId === childId &&
          (child.reservations.length > 0 ||
            child.scheduleType !== 'RESERVATION_REQUIRED') &&
          child.absence === null
      )
    )
  )

const hasNoReservationsForSomeChild = (
  calendarDays: ReservationResponseDay[],
  selectedChildren: string[]
) =>
  calendarDays.every((day) =>
    selectedChildren.some((childId) =>
      day.children.some(
        (child) =>
          child.childId === childId &&
          child.scheduleType === 'RESERVATION_REQUIRED' &&
          child.reservations.length === 0 &&
          child.absence === null
      )
    )
  )

const allChildrenAreAbsent = (
  calendarDays: ReservationResponseDay[],
  selectedChildren: string[]
) =>
  calendarDays.every((day) =>
    selectedChildren.every((childId) =>
      day.children.some(
        (child) => child.childId === childId && child.absence !== null
      )
    )
  )

const allChildrenAreAbsentNotEditable = (
  calendarDays: ReservationResponseDay[],
  selectedChildren: string[]
) =>
  calendarDays.every((day) =>
    selectedChildren.every((childId) =>
      day.children.some(
        (child) =>
          child.childId === childId &&
          child.absence !== null &&
          !child.absence.editable
      )
    )
  )

const allChildrenOnTermBreak = (
  calendarDays: ReservationResponseDay[],
  selectedChildren: string[]
) =>
  calendarDays.every((day) =>
    selectedChildren.every((childId) =>
      day.children.some(
        (child) =>
          child.childId === childId && child.scheduleType === 'TERM_BREAK'
      )
    )
  )

const reservationNotRequiredForAnyChild = (
  calendarDays: ReservationResponseDay[],
  selectedChildren: string[]
) =>
  calendarDays.every((day) =>
    selectedChildren.every((childId) =>
      day.children.some(
        (child) =>
          child.childId === childId &&
          child.scheduleType !== 'RESERVATION_REQUIRED'
      )
    )
  )

const bindUnboundedTimeRanges = (
  ranges: TimeRange[],
  validRange: TimeRange
): StateOf<LimitedLocalTimeRangeField>[] => {
  const formatted = ranges.map(({ start, end }) => ({
    value: { startTime: start.format(), endTime: end.format() },
    validRange
  }))

  if (ranges.length === 1 || ranges.length === 2) {
    return formatted
  }
  throw Error(`${ranges.length} time ranges when 1-2 expected`)
}

const getCommonTimeRanges = (
  calendarDays: ReservationResponseDay[],
  childIds: string[]
): TimeRange[] | undefined => {
  const uniqueRanges = uniqBy(
    calendarDays
      .flatMap((reservations) =>
        reservations.children
          .filter(({ childId }) => childIds.includes(childId))
          .map((child) =>
            child.reservations.flatMap((r): TimeRange[] =>
              r.type === 'TIMES' ? [new TimeRange(r.startTime, r.endTime)] : []
            )
          )
      )
      .filter((ranges) => ranges.length > 0),
    (times) => JSON.stringify(times)
  )

  if (uniqueRanges.length === 1) {
    return uniqueRanges[0]
  }

  return undefined
}

type HolidayPeriodState = 'open' | 'closed' | undefined

function timeRangeIntersection(ranges: TimeRange[]): TimeRange {
  return ranges.reduce(
    (acc, range) =>
      new TimeRange(
        range.start.isAfter(acc.start) ? range.start : acc.start,
        range.end.isBefore(acc.end) ? range.end : acc.end
      ),
    MAX_TIME_RANGE
  )
}

export class DayProperties {
  private readonly reservableDaysByChild: Record<UUID, Set<string> | undefined>
  private readonly combinedOperationDays: Set<number>

  minDate: LocalDate | undefined
  maxDate: LocalDate | undefined

  constructor(
    public readonly calendarDays: ReservationResponseDay[],
    reservableRange: FiniteDateRange,
    private readonly holidayPeriods: HolidayPeriodInfo[]
  ) {
    const reservableDaysByChild: Record<UUID, Set<string> | undefined> = {}
    const combinedOperationDays = new Set<number>()
    const holidays = new Set<string>()

    let minDate: LocalDate | undefined = undefined
    let maxDate: LocalDate | undefined = undefined

    calendarDays.forEach((day) => {
      if (reservableRange.includes(day.date) && day.children.length > 0) {
        if (!minDate || day.date.isBefore(minDate)) {
          minDate = day.date
        }
        if (!maxDate || day.date.isAfter(maxDate)) {
          maxDate = day.date
        }
      }

      const dayOfWeek = day.date.getIsoDayOfWeek()
      if (day.holiday) {
        holidays.add(day.date.formatIso())
      }
      day.children.forEach((child) => {
        combinedOperationDays.add(dayOfWeek)

        let childReservableDays = reservableDaysByChild[child.childId]
        if (!childReservableDays) {
          childReservableDays = new Set()
          reservableDaysByChild[child.childId] = childReservableDays
        }
        childReservableDays.add(day.date.formatIso())
      })
    })

    this.minDate = minDate
    this.maxDate = maxDate
    this.calendarDays = calendarDays
    this.reservableDaysByChild = reservableDaysByChild
    this.combinedOperationDays = combinedOperationDays
  }

  isOperationalDayForAnyChild(dayOfWeek: number): boolean {
    return this.combinedOperationDays.has(dayOfWeek)
  }

  getReservableDatesInRangeForChild(range: FiniteDateRange, childId: UUID) {
    const reservableDays = this.reservableDaysByChild[childId]
    if (!reservableDays) return []

    return [...range.dates()].filter((date) =>
      reservableDays.has(date.formatIso())
    )
  }

  holidayPeriodStateForDate(date: LocalDate): HolidayPeriodState {
    return this.holidayPeriods.find((holidayPeriod) =>
      holidayPeriod.period.includes(date)
    )?.state
  }

  holidayPeriodStateForRange(dateRange: FiniteDateRange): HolidayPeriodState {
    return this.holidayPeriods.find((holidayPeriod) =>
      holidayPeriod.period.contains(dateRange)
    )?.state
  }
}
