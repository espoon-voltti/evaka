// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import uniqBy from 'lodash/uniqBy'

import FiniteDateRange from 'lib-common/finite-date-range'
import { localDateRange, localTimeRange, string } from 'lib-common/form/fields'
import {
  array,
  mapped,
  object,
  oneOf,
  required,
  union,
  validated,
  value
} from 'lib-common/form/form'
import { StateOf } from 'lib-common/form/types'
import {
  DailyReservationRequest,
  ReservationChild,
  ReservationResponseDay
} from 'lib-common/generated/api-types/reservations'
import { TimeRange } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { Repetition } from 'lib-common/reservations'
import { UUID } from 'lib-common/types'
import { Translations } from 'lib-customizations/citizen'

export const emptyTimeRange: StateOf<typeof localTimeRange> = {
  startTime: '',
  endTime: ''
}

export const timeRanges = mapped(
  array(
    validated(localTimeRange, (output) =>
      // 00:00 is not a valid end time
      output !== undefined && output.end.hour === 0 && output.end.minute === 0
        ? 'timeFormat'
        : undefined
    )
  ),
  (output) => {
    const nonEmpty = output.flatMap((x) => x ?? [])
    return nonEmpty.length === 0
      ? undefined // All inputs empty => no value
      : nonEmpty
  }
)

export const reservation = union({
  timeRanges,
  absent: value<true>()
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
    readOnly: value<'notEditable' | 'holiday' | undefined>(),
    reservation,
    reservationNoTimes: noTimes
  }),
  ({ branch, value }): DayOutput => {
    switch (branch) {
      case 'readOnly':
        return { type: 'readOnly' }
      case 'reservation': {
        switch (value.branch) {
          case 'timeRanges':
            return value.value === undefined
              ? { type: 'nothing' }
              : {
                  type: 'reservations',
                  first: value.value[0],
                  second: value.value[1]
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

const emptyDay: StateOf<typeof day> = {
  branch: 'reservation',
  state: {
    branch: 'timeRanges',
    state: [emptyTimeRange]
  }
}

export const dailyTimes = object({
  weekDayRange: value<[number, number] | undefined>(),
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

export const timesUnion = union({
  dailyTimes,
  weeklyTimes,
  irregularTimes
})

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
    dateRange: required(localDateRange),
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
  isOpen: boolean
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
    dateRange: {
      startDate: initialStart,
      endDate: initialEnd
    },
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
        : {
            branch: 'dailyTimes',
            state: {
              weekDayRange: undefined,
              day: emptyDay
            }
          }
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
    const weekDayRange: [number, number] | undefined =
      includedWeekDays.length > 0
        ? [includedWeekDays[0], includedWeekDays[includedWeekDays.length - 1]]
        : undefined

    const isOpenHolidayPeriod =
      dayProperties.containedInOpenHolidayPeriod(selectedRange)

    return {
      branch: 'dailyTimes',
      state: {
        weekDayRange,
        day: resetDay(
          isOpenHolidayPeriod,
          calendarDaysInRange,
          selectedChildren
        )
      }
    }
  } else if (repetition === 'WEEKLY') {
    const groupedDays = groupBy(selectedRangeDates, (date) =>
      date.getIsoDayOfWeek()
    )

    const isOpenHolidayPeriod =
      dayProperties.containedInOpenHolidayPeriod(selectedRange)

    const weeklyTimes = includedWeekDays.map(
      (dayOfWeek): StateOf<typeof weekDay> => {
        const dayOfWeekDays = groupedDays[dayOfWeek]
        if (!dayOfWeekDays) {
          return {
            weekDay: dayOfWeek,
            day: { branch: 'readOnly', state: undefined }
          }
        }

        const relevantCalendarDays = calendarDaysInRange.filter(({ date }) =>
          dayOfWeekDays.some((d) => d.isEqual(date))
        )
        return {
          weekDay: dayOfWeek,
          day: resetDay(
            isOpenHolidayPeriod,
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

        const isOpenHolidayPeriod = dayProperties.inOpenHolidayPeriod(rangeDate)
        return {
          date: rangeDate,
          day: resetDay(isOpenHolidayPeriod, [calendarDay], selectedChildren)
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
  isOpenHolidayPeriod: boolean,
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
      state: undefined
    }
  }

  if (allChildrenAreAbsentNotEditable(calendarDays, selectedChildren)) {
    return {
      branch: 'readOnly',
      state: 'notEditable'
    }
  }

  if (allChildrenAreAbsent(calendarDays, selectedChildren)) {
    return isOpenHolidayPeriod || reservationNotRequired
      ? { branch: 'reservationNoTimes', state: 'absent' }
      : { branch: 'reservation', state: { branch: 'absent', state: true } }
  }

  if (hasReservationsForEveryChild(calendarDays, selectedChildren)) {
    if (isOpenHolidayPeriod || reservationNotRequired) {
      return { branch: 'reservationNoTimes', state: 'present' }
    }

    const commonTimeRanges = getCommonTimeRanges(calendarDays, selectedChildren)
    if (commonTimeRanges) {
      return {
        branch: 'reservation',
        state: {
          branch: 'timeRanges',
          state: bindUnboundedTimeRanges(commonTimeRanges)
        }
      }
    }
  }

  return isOpenHolidayPeriod || reservationNotRequired
    ? { branch: 'reservationNoTimes', state: 'notSet' }
    : emptyDay
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
          (child.reservations.length > 0 || !child.requiresReservation) &&
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

const reservationNotRequiredForAnyChild = (
  calendarDays: ReservationResponseDay[],
  selectedChildren: string[]
) =>
  calendarDays.every((day) =>
    selectedChildren.every((childId) =>
      day.children.some(
        (child) => child.childId === childId && !child.requiresReservation
      )
    )
  )

const bindUnboundedTimeRanges = (
  ranges: TimeRange[]
): StateOf<typeof localTimeRange>[] => {
  const formatted = ranges.map(({ start, end }) => ({
    startTime: start.format(),
    endTime: end.format()
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
              r.type === 'TIMES' ? [{ start: r.startTime, end: r.endTime }] : []
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

export class DayProperties {
  // Does any child have shift care?
  readonly anyChildInShiftCare: boolean

  private readonly reservableDaysByChild: Record<UUID, Set<string> | undefined>
  private readonly combinedOperationDays: Set<number>

  constructor(
    public readonly calendarDays: ReservationResponseDay[],
    private readonly holidayPeriods: HolidayPeriodInfo[]
  ) {
    let anyChildInShiftCare = false
    const reservableDaysByChild: Record<UUID, Set<string> | undefined> = {}
    const combinedOperationDays = new Set<number>()
    const holidays = new Set<string>()

    calendarDays.forEach((day) => {
      const dayOfWeek = day.date.getIsoDayOfWeek()
      if (day.holiday) {
        holidays.add(day.date.formatIso())
      }
      day.children.forEach((child) => {
        combinedOperationDays.add(dayOfWeek)

        anyChildInShiftCare = anyChildInShiftCare || child.shiftCare

        let childReservableDays = reservableDaysByChild[child.childId]
        if (!childReservableDays) {
          childReservableDays = new Set()
          reservableDaysByChild[child.childId] = childReservableDays
        }
        childReservableDays.add(day.date.formatIso())
      })
    })

    this.calendarDays = calendarDays
    this.anyChildInShiftCare = anyChildInShiftCare
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

  inOpenHolidayPeriod(date: LocalDate): boolean {
    return this.holidayPeriods.some(
      (holidayPeriod) =>
        holidayPeriod.isOpen && holidayPeriod.period.includes(date)
    )
  }

  containedInOpenHolidayPeriod(dateRange: FiniteDateRange): boolean {
    return this.holidayPeriods.some(
      (holidayPeriod) =>
        holidayPeriod.isOpen && holidayPeriod.period.contains(dateRange)
    )
  }
}
