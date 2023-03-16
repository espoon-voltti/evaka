// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import uniqBy from 'lodash/uniqBy'

import FiniteDateRange from 'lib-common/finite-date-range'
import {
  boolean,
  localDateRange,
  localTimeRange,
  string
} from 'lib-common/form/fields'
import {
  array,
  chained,
  mapped,
  object,
  oneOf,
  required,
  validated,
  value
} from 'lib-common/form/form'
import { StateOf, ValidationSuccess } from 'lib-common/form/types'
import {
  DailyReservationData,
  ReservationChild,
  TimeRange
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { Repetition } from 'lib-common/reservations'
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
export const day = chained(
  object({
    present: boolean(),
    times
  }),
  (form, state) =>
    state.present
      ? form.shape.times.validate(state.times).map((times) => ({
          present: true,
          times
        }))
      : ValidationSuccess.of({ present: false, times: [] })
)

const emptyDay: StateOf<typeof day> = {
  present: true,
  times: [emptyTimeRange]
}

export const weekDay = chained(
  object({
    mode: value<'normal' | 'not-editable' | undefined>(),
    day
  }),
  (form, state) =>
    state.mode !== 'normal'
      ? ValidationSuccess.of(undefined)
      : form.shape.day.validate(state.day)
)
export const weeklyTimes = array(weekDay)

export const irregularDay = chained(
  object({
    date: value<LocalDate>(),
    mode: value<'normal' | 'not-editable' | 'holiday'>(),
    day
  }),
  (form, state) =>
    state.mode === 'normal'
      ? form.shape.day.validate(state.day).map((day) => ({
          date: state.date,
          day
        }))
      : ValidationSuccess.of(undefined)
)

export const irregularTimes = array(irregularDay)

export const reservationForm = mapped(
  object({
    selectedChildren: validated(array(string()), (value) =>
      value.length > 0 ? undefined : 'required'
    ),
    dateRange: required(localDateRange),
    repetition: required(oneOf<Repetition>()),
    dailyTimes: times,
    weeklyTimes,
    irregularTimes
  }),
  (output) => ({
    containsNonReservableDays: (
      reservableDays: Record<string, FiniteDateRange[]>
    ) =>
      !output.selectedChildren.every((childId) => {
        const childReservableDays = reservableDays[childId] ?? []
        return [...output.dateRange.dates()].every((date) =>
          childReservableDays.some((range) => range.includes(date))
        )
      }),
    toRequest: (reservableDays: Record<string, FiniteDateRange[]>) =>
      output.selectedChildren
        .flatMap((childId) => {
          const childReservableDays = reservableDays[childId] ?? []
          const dates = [...output.dateRange.dates()].filter((date) =>
            childReservableDays.some((range) => range.includes(date))
          )
          switch (output.repetition) {
            case 'DAILY':
              return dates.map((date) => ({
                childId,
                date,
                reservations: output.dailyTimes,
                absent: false
              }))
            case 'WEEKLY':
              return dates
                .map((date) => {
                  const weekDay = output.weeklyTimes[date.getIsoDayOfWeek() - 1]
                  return weekDay
                    ? {
                        childId,
                        date,
                        reservations: weekDay.present ? weekDay.times : null,
                        absent: !weekDay.present
                      }
                    : undefined
                })
                .flatMap((day) => (day ? [day] : []))
            case 'IRREGULAR':
              return output.irregularTimes
                .flatMap((irregularDay) =>
                  irregularDay !== undefined ? [irregularDay] : []
                )
                .filter((irregularDay) => {
                  return output.dateRange.includes(irregularDay.date)
                })
                .map((irregularDay) => ({
                  childId,
                  date: irregularDay.date,
                  reservations: irregularDay.day.present
                    ? irregularDay.day.times
                    : null,
                  absent: !irregularDay.day.present
                }))
          }
        })
        .filter(
          ({ reservations, absent }) =>
            (reservations && reservations.length > 0) || absent
        )
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

export function initialState(
  availableChildren: ReservationChild[],
  initialStart: LocalDate | null,
  initialEnd: LocalDate | null,
  i18n: Translations
): StateOf<typeof reservationForm> {
  return {
    selectedChildren: availableChildren.map((child) => child.id),
    dateRange: {
      startDate: initialStart,
      endDate: initialEnd
    },
    repetition: {
      domValue: 'DAILY' as const,
      options: repetitionOptions(i18n)
    },
    dailyTimes: [
      {
        startTime: '',
        endTime: ''
      }
    ],
    weeklyTimes: [],
    irregularTimes: []
  }
}

export function resetTimes(
  childrenInShiftCare: boolean,
  existingReservations: DailyReservationData[],
  repetition: Repetition,
  selectedRange: FiniteDateRange,
  selectedChildren: UUID[]
): {
  dailyTimes: StateOf<typeof times>
  weeklyTimes: StateOf<typeof weeklyTimes>
  irregularTimes: StateOf<typeof irregularTimes>
} {
  const reservations = existingReservations.filter((reservation) =>
    selectedRange.includes(reservation.date)
  )

  if (repetition === 'DAILY') {
    if (!hasReservationsForEveryChild(reservations, selectedChildren)) {
      return {
        dailyTimes: [emptyTimeRange],
        weeklyTimes: [],
        irregularTimes: []
      }
    }

    const commonTimeRanges = getCommonTimeRanges(reservations, selectedChildren)

    if (commonTimeRanges) {
      return {
        dailyTimes: bindUnboundedTimeRanges(commonTimeRanges),
        weeklyTimes: [],
        irregularTimes: []
      }
    }

    return {
      dailyTimes: [emptyTimeRange],
      weeklyTimes: [],
      irregularTimes: []
    }
  } else if (repetition === 'WEEKLY') {
    const groupedDays = groupBy(
      [...selectedRange.dates()],
      (date) => date.getIsoDayOfWeek() - 1
    )

    const weeklyTimes = Array.from({ length: 7 }).map((_, dayOfWeek) => {
      const dayOfWeekDays = groupedDays[dayOfWeek]
      if (!dayOfWeekDays) {
        return { mode: undefined, day: emptyDay }
      }

      const relevantReservations = reservations.filter(({ date }) =>
        dayOfWeekDays.some((d) => d.isEqual(date))
      )

      if (
        allChildrenAreAbsentMarkedByEmployee(
          relevantReservations,
          selectedChildren
        )
      ) {
        return { mode: 'not-editable' as const, day: emptyDay }
      }

      if (allChildrenAreAbsent(relevantReservations, selectedChildren)) {
        return {
          mode: 'normal' as const,
          day: { present: false, times: [emptyTimeRange] }
        }
      }

      if (
        !hasReservationsForEveryChild(relevantReservations, selectedChildren)
      ) {
        return { mode: 'normal' as const, day: emptyDay }
      }

      const commonTimeRanges = getCommonTimeRanges(
        relevantReservations,
        selectedChildren
      )

      if (commonTimeRanges) {
        return {
          mode: 'normal' as const,
          day: {
            present: true,
            times: bindUnboundedTimeRanges(commonTimeRanges)
          }
        }
      }

      return { mode: 'normal' as const, day: emptyDay }
    })
    return {
      dailyTimes: [],
      weeklyTimes,
      irregularTimes: []
    }
  } else if (repetition === 'IRREGULAR') {
    const irregularTimes = [...selectedRange.dates()].map(
      (rangeDate): StateOf<typeof irregularDay> => {
        const existingTimes = reservations.find(({ date }) =>
          rangeDate.isEqual(date)
        )

        if (!existingTimes) {
          return { date: rangeDate, mode: 'normal' as const, day: emptyDay }
        }

        if (existingTimes.isHoliday && !childrenInShiftCare) {
          return { date: rangeDate, mode: 'holiday' as const, day: emptyDay }
        }

        if (
          allChildrenAreAbsentMarkedByEmployee(
            [existingTimes],
            selectedChildren
          )
        ) {
          return {
            date: rangeDate,
            mode: 'not-editable' as const,
            day: emptyDay
          }
        }

        if (allChildrenAreAbsent([existingTimes], selectedChildren)) {
          return {
            date: rangeDate,
            mode: 'normal' as const,
            day: { present: false, times: [emptyTimeRange] }
          }
        }

        if (!hasReservationsForEveryChild([existingTimes], selectedChildren)) {
          return { date: rangeDate, mode: 'normal' as const, day: emptyDay }
        }

        const commonTimeRanges = getCommonTimeRanges(
          [existingTimes],
          selectedChildren
        )

        if (commonTimeRanges) {
          return {
            date: rangeDate,
            mode: 'normal' as const,
            day: {
              present: true,
              times: bindUnboundedTimeRanges(commonTimeRanges)
            }
          }
        }

        return { date: rangeDate, mode: 'normal' as const, day: emptyDay }
      }
    )
    return {
      dailyTimes: [],
      weeklyTimes: [],
      irregularTimes
    }
  } else {
    throw new Error('Not reached')
  }
}

const hasReservationsForEveryChild = (
  dayData: DailyReservationData[],
  childIds: string[]
) =>
  dayData.every((reservations) =>
    childIds.every((childId) =>
      reservations.children.some(
        (child) => child.childId === childId && child.reservations.length > 0
      )
    )
  )

const allChildrenAreAbsent = (
  dayData: DailyReservationData[],
  childIds: string[]
) =>
  dayData.every((reservations) =>
    childIds.every((childId) =>
      reservations.children.some(
        (child) => child.childId === childId && !!child.absence
      )
    )
  )

const allChildrenAreAbsentMarkedByEmployee = (
  dayData: DailyReservationData[],
  childIds: string[]
) =>
  dayData.every((reservations) =>
    childIds.every((childId) =>
      reservations.children.some(
        (child) =>
          child.childId === childId && !!child.absence && child.markedByEmployee
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
  dayData: DailyReservationData[],
  childIds: string[]
): TimeRange[] | undefined => {
  const uniqueRanges = uniqBy(
    dayData
      .flatMap((reservations) =>
        reservations.children
          .filter(({ childId }) => childIds.includes(childId))
          .map((child) => child.reservations)
      )
      .filter((ranges) => ranges.length > 0),
    (times) => JSON.stringify(times)
  )

  if (uniqueRanges.length === 1) {
    return uniqueRanges[0]
  }

  return undefined
}
