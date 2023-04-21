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
  Reservation,
  ReservationChild,
  ReservationResponseDay
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import {
  Repetition,
  TimeRange,
  timeRangeToTimes
} from 'lib-common/reservations'
import { UUID } from 'lib-common/types'
import { Translations } from 'lib-customizations/citizen'

export const emptyTimeRange: StateOf<typeof localTimeRange> = {
  startTime: '',
  endTime: ''
}

export const times = array(
  validated(required(localTimeRange), ({ endTime }) =>
    // 00:00 is not a valid end time
    endTime.hour === 0 && endTime.minute === 0 ? 'timeFormat' : undefined
  )
)

export const day = mapped(
  union({
    readOnly: value<'not-editable' | 'holiday' | undefined>(),
    reservation: times,
    holidayReservation: value<'present' | 'absent' | 'not-set'>()
  }),
  ({ branch, value }): Reservation[] | undefined =>
    branch === 'reservation'
      ? value.map(timeRangeToTimes)
      : branch === 'holidayReservation'
      ? value === 'present'
        ? [{ type: 'NO_TIMES' }]
        : value === 'absent'
        ? []
        : undefined // not-set
      : undefined // readOnly
)

const emptyDay: StateOf<typeof day> = {
  branch: 'reservation',
  state: [emptyTimeRange]
}

export const dailyTimes = mapped(
  object({
    weekDayRange: value<[number, number] | undefined>(),
    reservation: union({
      times,
      holidayReservation: value<'present' | 'absent' | 'not-set'>()
    })
  }),
  (output): Reservation[] | undefined =>
    output.reservation.branch === 'times'
      ? output.reservation.value.map(timeRangeToTimes)
      : output.reservation.value === 'present'
      ? [{ type: 'NO_TIMES' }]
      : output.reservation.value === 'absent'
      ? []
      : undefined // not-set
)

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

function toDailyReservationRequest(
  childId: UUID,
  date: LocalDate,
  reservations: Reservation[] | undefined
): DailyReservationRequest {
  return reservations === undefined
    ? {
        type: 'NOTHING',
        childId,
        date
      }
    : reservations.length === 0
    ? {
        type: 'ABSENCE',
        childId,
        date
      }
    : {
        type: 'RESERVATIONS',
        childId,
        date,
        reservation: reservations[0],
        secondReservation: reservations[1] ?? null
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
    containsNonReservableDays: (dayProperties: DayProperties) =>
      !output.selectedChildren.every((childId) =>
        dayProperties.isWholeRangeReservableForChild(output.dateRange, childId)
      ),
    toRequest: (dayProperties: DayProperties): DailyReservationRequest[] =>
      output.selectedChildren.flatMap((childId): DailyReservationRequest[] => {
        const dates = dayProperties.getReservableDatesInRangeForChild(
          output.dateRange,
          childId
        )
        switch (output.times.branch) {
          case 'dailyTimes': {
            const reservations = output.times.value
            return dates.map((date) =>
              toDailyReservationRequest(childId, date, reservations)
            )
          }
          case 'weeklyTimes': {
            const timesValue = output.times.value
            return dates.map((date) => {
              const reservations = timesValue.find(
                (x) => x.weekDay === date.getIsoDayOfWeek()
              )?.day
              return toDailyReservationRequest(childId, date, reservations)
            })
          }
          case 'irregularTimes':
            return output.times.value
              .filter((irregularDay) => {
                return output.dateRange.includes(irregularDay.date)
              })
              .map(({ date, day }) =>
                toDailyReservationRequest(childId, date, day)
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
  holidayPeriods: HolidayPeriodInfo[],
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
        ? resetTimes(
            dayProperties,
            'IRREGULAR',
            new FiniteDateRange(initialStart, initialEnd),
            selectedChildren,
            holidayPeriods
          )
        : {
            branch: 'dailyTimes',
            state: {
              weekDayRange: undefined,
              reservation: {
                branch: 'times',
                state: [emptyTimeRange]
              }
            }
          }
  }
}

export function resetTimes(
  dayProperties: DayProperties,
  repetition: Repetition,
  selectedRange: FiniteDateRange,
  selectedChildren: UUID[],
  holidayPeriods: HolidayPeriodInfo[]
): StateOf<typeof reservationForm>['times'] {
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

    const isOpenHolidayPeriod = holidayPeriods.some(
      (period) => period.isOpen && period.period.contains(selectedRange)
    )

    if (
      isOpenHolidayPeriod &&
      allChildrenAreAbsent(calendarDaysInRange, selectedChildren)
    ) {
      return {
        branch: 'dailyTimes',
        state: {
          weekDayRange,
          reservation: { branch: 'holidayReservation', state: 'absent' }
        }
      }
    }

    if (hasReservationsForEveryChild(calendarDaysInRange, selectedChildren)) {
      if (isOpenHolidayPeriod) {
        return {
          branch: 'dailyTimes',
          state: {
            weekDayRange,
            reservation: { branch: 'holidayReservation', state: 'present' }
          }
        }
      }

      const commonTimeRanges = getCommonTimeRanges(
        calendarDaysInRange,
        selectedChildren
      )

      if (commonTimeRanges) {
        return {
          branch: 'dailyTimes',
          state: {
            weekDayRange,
            reservation: {
              branch: 'times',
              state: bindUnboundedTimeRanges(commonTimeRanges)
            }
          }
        }
      }
    }

    return {
      branch: 'dailyTimes',
      state: {
        weekDayRange,
        reservation: isOpenHolidayPeriod
          ? { branch: 'holidayReservation', state: 'not-set' }
          : {
              branch: 'times',
              state: [emptyTimeRange]
            }
      }
    }
  } else if (repetition === 'WEEKLY') {
    const groupedDays = groupBy(selectedRangeDates, (date) =>
      date.getIsoDayOfWeek()
    )

    const isOpenHolidayPeriod = holidayPeriods.some(
      (holidayPeriod) =>
        holidayPeriod.isOpen && holidayPeriod.period.contains(selectedRange)
    )

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

        if (
          holidayWithNoChildrenInShiftCare(
            relevantCalendarDays,
            selectedChildren
          )
        ) {
          return {
            weekDay: dayOfWeek,
            day: { branch: 'readOnly', state: 'holiday' }
          }
        }

        if (
          allChildrenAreAbsentMarkedByEmployee(
            relevantCalendarDays,
            selectedChildren
          )
        ) {
          return {
            weekDay: dayOfWeek,
            day: { branch: 'readOnly', state: 'not-editable' }
          }
        }

        if (allChildrenAreAbsent(relevantCalendarDays, selectedChildren)) {
          return {
            weekDay: dayOfWeek,
            day: isOpenHolidayPeriod
              ? { branch: 'holidayReservation', state: 'absent' }
              : {
                  branch: 'reservation',
                  state: []
                }
          }
        }

        if (
          hasReservationsForEveryChild(relevantCalendarDays, selectedChildren)
        ) {
          if (isOpenHolidayPeriod) {
            return {
              weekDay: dayOfWeek,
              day: { branch: 'holidayReservation', state: 'present' }
            }
          }

          const commonTimeRanges = getCommonTimeRanges(
            relevantCalendarDays,
            selectedChildren
          )

          if (commonTimeRanges)
            return {
              weekDay: dayOfWeek,
              day: {
                branch: 'reservation',
                state: bindUnboundedTimeRanges(commonTimeRanges)
              }
            }
        }
        return {
          weekDay: dayOfWeek,
          day: isOpenHolidayPeriod
            ? { branch: 'holidayReservation', state: 'not-set' }
            : emptyDay
        }
      }
    )
    return {
      branch: 'weeklyTimes',
      state: weeklyTimes
    }
  } else if (repetition === 'IRREGULAR') {
    const irregularTimes = calendarDaysInRange.map(
      (calendarDay): StateOf<typeof irregularDay> => {
        const rangeDate = calendarDay.date
        const dayChildren = calendarDay.children.filter((dayChild) =>
          selectedChildren.includes(dayChild.childId)
        )

        if (dayChildren.length === 0) {
          return {
            date: rangeDate,
            day: { branch: 'readOnly', state: undefined }
          }
        }

        const isOpenHolidayPeriod = holidayPeriods.some(
          (holidayPeriod) =>
            holidayPeriod.isOpen && holidayPeriod.period.includes(rangeDate)
        )

        if (holidayWithNoChildrenInShiftCare([calendarDay], selectedChildren)) {
          return {
            date: rangeDate,
            day: { branch: 'readOnly', state: 'holiday' }
          }
        }

        if (
          allChildrenAreAbsentMarkedByEmployee([calendarDay], selectedChildren)
        ) {
          return {
            date: rangeDate,
            day: {
              branch: 'readOnly',
              state: 'not-editable'
            }
          }
        }

        if (allChildrenAreAbsent([calendarDay], selectedChildren)) {
          return {
            date: rangeDate,
            day: isOpenHolidayPeriod
              ? { branch: 'holidayReservation', state: 'absent' }
              : {
                  branch: 'reservation',
                  state: []
                }
          }
        }

        if (hasReservationsForEveryChild([calendarDay], selectedChildren)) {
          if (isOpenHolidayPeriod) {
            return {
              date: rangeDate,
              day: { branch: 'holidayReservation', state: 'present' }
            }
          } else {
            const commonTimeRanges = getCommonTimeRanges(
              [calendarDay],
              selectedChildren
            )
            if (commonTimeRanges) {
              return {
                date: rangeDate,
                day: {
                  branch: 'reservation',
                  state: bindUnboundedTimeRanges(commonTimeRanges)
                }
              }
            }
          }
        }
        return {
          date: rangeDate,
          day: isOpenHolidayPeriod
            ? { branch: 'holidayReservation', state: 'not-set' }
            : emptyDay
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
        (child) => child.childId === childId && child.reservations.length > 0
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

const allChildrenAreAbsentMarkedByEmployee = (
  calendarDays: ReservationResponseDay[],
  selectedChildren: string[]
) =>
  calendarDays.every((day) =>
    selectedChildren.every((childId) =>
      day.children.some(
        (child) =>
          child.childId === childId &&
          (child.absence?.markedByEmployee ?? false)
      )
    )
  )

const bindUnboundedTimeRanges = (
  ranges: TimeRange[]
): StateOf<typeof localTimeRange>[] => {
  const formatted = ranges.map(({ startTime, endTime }) => ({
    startTime: startTime.format(),
    endTime: endTime.format()
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
              r.type === 'TIMES'
                ? [
                    {
                      startTime: r.startTime,
                      endTime: r.endTime
                    }
                  ]
                : []
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
  readonly calendarDays: ReservationResponseDay[]

  // Does any child have shift care?
  readonly anyChildInShiftCare: boolean

  private readonly reservableDaysByChild: Record<UUID, Set<string> | undefined>
  private readonly combinedOperationDays: Set<number>
  private readonly includesWeekends: boolean

  constructor(
    calendarDays: ReservationResponseDay[],
    includesWeekends: boolean
  ) {
    let anyChildInShiftCare = false
    const reservableDaysByChild: Record<UUID, Set<string> | undefined> = {}
    const combinedOperationDays = new Set<number>()

    calendarDays.forEach((day) => {
      const dayOfWeek = day.date.getIsoDayOfWeek()
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
    this.includesWeekends = includesWeekends
  }

  isOperationalDayForAnyChild(dayOfWeek: number): boolean {
    return this.combinedOperationDays.has(dayOfWeek)
  }

  isWholeRangeReservableForChild(
    range: FiniteDateRange,
    childId: UUID
  ): boolean {
    const reservableDays = this.reservableDaysByChild[childId]
    if (!reservableDays) return false

    return [...range.dates()].every((date) =>
      // Skip weekend days in range if the calendar doesn't include weekends
      !this.includesWeekends && date.isWeekend()
        ? true
        : reservableDays.has(date.formatIso())
    )
  }

  getReservableDatesInRangeForChild(range: FiniteDateRange, childId: UUID) {
    const reservableDays = this.reservableDaysByChild[childId]
    if (!reservableDays) return []

    return [...range.dates()].filter((date) =>
      reservableDays.has(date.formatIso())
    )
  }
}
