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
  union,
  validated,
  value
} from 'lib-common/form/form'
import { StateOf, ValidationSuccess } from 'lib-common/form/types'
import {
  DailyReservationData,
  DailyReservationRequest,
  ReservationChild
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

export const writableDay = chained(
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

export const day = mapped(
  union({
    readOnly: value<'not-editable' | 'holiday' | undefined>(),
    writable: writableDay
  }),
  (value) => (value.branch === 'writable' ? value.value : undefined)
)

const emptyDay: StateOf<typeof day> = {
  branch: 'writable',
  state: {
    present: true,
    times: [emptyTimeRange]
  }
}

export const weeklyTimes = array(day)

export const irregularDay = mapped(
  object({
    date: value<LocalDate>(),
    day
  }),
  ({ date, day }) => (day !== undefined ? { date, day } : undefined)
)

export const irregularTimes = array(irregularDay)

export const timesUnion = union({
  dailyTimes: times,
  weeklyTimes,
  irregularTimes
})

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
        .flatMap((childId): DailyReservationRequest[] => {
          const childReservableDays = reservableDays[childId] ?? []
          const dates = [...output.dateRange.dates()].filter((date) =>
            childReservableDays.some((range) => range.includes(date))
          )
          switch (output.times.branch) {
            case 'dailyTimes': {
              const timesValue = output.times.value
              return dates.map((date) => ({
                childId,
                date,
                reservations: timesValue.map(timeRangeToTimes),
                absent: false
              }))
            }
            case 'weeklyTimes': {
              const timesValue = output.times.value
              return dates
                .map((date) => {
                  const weekDay = timesValue[date.getIsoDayOfWeek() - 1]
                  return weekDay
                    ? {
                        childId,
                        date,
                        reservations: weekDay.present
                          ? weekDay.times.map(timeRangeToTimes)
                          : null,
                        absent: !weekDay.present
                      }
                    : undefined
                })
                .flatMap((day) => (day ? [day] : []))
            }
            case 'irregularTimes': {
              return output.times.value
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
                    ? irregularDay.day.times.map(timeRangeToTimes)
                    : null,
                  absent: !irregularDay.day.present
                }))
            }
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
    times: {
      branch: 'dailyTimes',
      state: [emptyTimeRange]
    }
  }
}

export function resetTimes(
  childrenInShiftCare: boolean,
  existingReservations: DailyReservationData[],
  repetition: Repetition,
  selectedRange: FiniteDateRange,
  selectedChildren: UUID[]
): StateOf<typeof reservationForm>['times'] {
  const reservations = existingReservations.filter((reservation) =>
    selectedRange.includes(reservation.date)
  )

  if (repetition === 'DAILY') {
    if (!hasReservationsForEveryChild(reservations, selectedChildren)) {
      return {
        branch: 'dailyTimes',
        state: [emptyTimeRange]
      }
    }

    const commonTimeRanges = getCommonTimeRanges(reservations, selectedChildren)

    if (commonTimeRanges) {
      return {
        branch: 'dailyTimes',
        state: bindUnboundedTimeRanges(commonTimeRanges)
      }
    }

    return {
      branch: 'dailyTimes',
      state: [emptyTimeRange]
    }
  } else if (repetition === 'WEEKLY') {
    const groupedDays = groupBy(
      [...selectedRange.dates()],
      (date) => date.getIsoDayOfWeek() - 1
    )

    const weeklyTimes = Array.from({ length: 7 }).map(
      (_, dayOfWeek): StateOf<typeof day> => {
        const dayOfWeekDays = groupedDays[dayOfWeek]
        if (!dayOfWeekDays) {
          return { branch: 'readOnly', state: undefined }
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
          return { branch: 'readOnly', state: 'not-editable' }
        }

        if (allChildrenAreAbsent(relevantReservations, selectedChildren)) {
          return {
            branch: 'writable',
            state: { present: false, times: [emptyTimeRange] }
          }
        }

        if (
          !hasReservationsForEveryChild(relevantReservations, selectedChildren)
        ) {
          return emptyDay
        }

        const commonTimeRanges = getCommonTimeRanges(
          relevantReservations,
          selectedChildren
        )

        if (commonTimeRanges) {
          return {
            branch: 'writable',
            state: {
              present: true,
              times: bindUnboundedTimeRanges(commonTimeRanges)
            }
          }
        }

        return emptyDay
      }
    )
    return {
      branch: 'weeklyTimes',
      state: weeklyTimes
    }
  } else if (repetition === 'IRREGULAR') {
    const irregularTimes = [...selectedRange.dates()].map(
      (rangeDate): StateOf<typeof irregularDay> => {
        const existingTimes = reservations.find(({ date }) =>
          rangeDate.isEqual(date)
        )

        if (!existingTimes) {
          return {
            date: rangeDate,
            day: emptyDay
          }
        }

        if (existingTimes.isHoliday && !childrenInShiftCare) {
          return {
            date: rangeDate,
            day: { branch: 'readOnly', state: 'holiday' }
          }
        }

        if (
          allChildrenAreAbsentMarkedByEmployee(
            [existingTimes],
            selectedChildren
          )
        ) {
          return {
            date: rangeDate,
            day: {
              branch: 'readOnly',
              state: 'not-editable'
            }
          }
        }

        if (allChildrenAreAbsent([existingTimes], selectedChildren)) {
          return {
            date: rangeDate,
            day: {
              branch: 'writable',
              state: { present: false, times: [emptyTimeRange] }
            }
          }
        }

        if (!hasReservationsForEveryChild([existingTimes], selectedChildren)) {
          return {
            date: rangeDate,
            day: emptyDay
          }
        }

        const commonTimeRanges = getCommonTimeRanges(
          [existingTimes],
          selectedChildren
        )

        if (commonTimeRanges) {
          return {
            date: rangeDate,
            day: {
              branch: 'writable',
              state: {
                present: true,
                times: bindUnboundedTimeRanges(commonTimeRanges)
              }
            }
          }
        }

        return { date: rangeDate, day: emptyDay }
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
