// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type {
  AbsenceInfo,
  ReservationResponse,
  ReservationResponseDay,
  ReservationResponseDayChild
} from 'lib-common/generated/api-types/reservations'
import type { ChildId } from 'lib-common/generated/api-types/shared'
import type { EvakaUser } from 'lib-common/generated/api-types/user'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { evakaUserId, fromUuid, randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import { DayProperties, resetTimes } from './form'

const monday = LocalDate.of(2021, 2, 1)
const tuesday = monday.addDays(1)
const wednesday = monday.addDays(2)
const sunday = monday.addDays(6)
const thursdayLastWeek = monday.subDays(4)
const tuesdayNextWeek = sunday.addDays(2)
const fridayNextWeek = sunday.addDays(5)

const reservableRange = new FiniteDateRange(
  LocalDate.of(2020, 8, 1),
  LocalDate.of(2021, 7, 31)
)

// mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
const selectedRange = new FiniteDateRange(monday, tuesdayNextWeek)

// mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
//          [] [] [] [] | [] [] [] [] [] [] [] | [] [] [] [] []
const emptyCalendarDaysIncludingWeekend: ReservationResponseDay[] = [
  ...new FiniteDateRange(thursdayLastWeek, fridayNextWeek).dates()
].map((date) => ({
  date,
  holiday: false,
  children: []
}))

const childId1 = randomId<ChildId>()
const childId2 = randomId<ChildId>()
const childId3 = randomId<ChildId>()
const childId4 = randomId<ChildId>()

// mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
//          [] []       | [] [] [] [] []       | [] [] [] [] []
const emptyCalendarDays = emptyCalendarDaysIncludingWeekend.filter(
  (day) => !day.date.isWeekend()
)

const defaultReservableTimeRange = new TimeRange(
  LocalTime.of(7, 0),
  LocalTime.of(17, 30)
)

const emptyChild: ReservationResponseDayChild = {
  childId: childId1,
  scheduleType: 'RESERVATION_REQUIRED',
  shiftCare: false,
  holidayPeriodEffect: null,
  absence: null,
  reservations: [],
  attendances: [],
  usedService: null,
  reservableTimeRange: {
    type: 'NORMAL',
    range: defaultReservableTimeRange
  }
}

const nullUUID = evakaUserId(fromUuid('00000000-0000-0000-0000-000000000000'))

const user: EvakaUser = {
  id: nullUUID,
  name: 'eVaka',
  type: 'SYSTEM'
}

const timeInputState = (
  startTime: string,
  endTime: string,
  validRange: TimeRange = defaultReservableTimeRange
) => ({ value: { startTime, endTime }, validRange })

describe('resetTimes', () => {
  describe('DAILY', () => {
    it('No reservable days', () => {
      // This doesn't happen in practice because selectedRange is limited
      // so that at least one child has a placement. It shouldn't break nevertheless.

      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          [] []       | [] [] [] [] []       | [] [] [] [] []
      const dayProperties = new DayProperties(
        emptyCalendarDays,
        reservableRange
      )

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'notInitialized',
        state: undefined
      })
    })

    it('One reservable day', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          [] []       | [] _  [] [] []       | [] [] [] [] []
      const calendarDays = [...emptyCalendarDays]
      calendarDays[3] = {
        date: tuesday,
        holiday: false,
        children: [emptyChild]
      }

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'dailyTimes',
        state: {
          weekDayRange: [2, 2],
          day: {
            branch: 'reservation',
            state: {
              validTimeRange: defaultReservableTimeRange,
              reservation: {
                branch: 'timeRanges',
                state: [timeInputState('', '')]
              }
            }
          }
        }
      })
    })

    it('All weekdays are reservable', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          _  _        | _  _  _  _  _        | _  _  _  _  _
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [emptyChild]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'dailyTimes',
        state: {
          weekDayRange: [1, 5],
          day: {
            branch: 'reservation',
            state: {
              validTimeRange: defaultReservableTimeRange,
              reservation: {
                branch: 'timeRanges',
                state: [timeInputState('', '')]
              }
            }
          }
        }
      })
    })

    it('All days are reservable', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          _  _  _  _  | _  _  _  _  _  _  _  | _  _  _  _  _
      const calendarDays: ReservationResponseDay[] =
        emptyCalendarDaysIncludingWeekend.map((day) => ({
          ...day,
          children: [emptyChild]
        }))

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'dailyTimes',
        state: {
          weekDayRange: [1, 7],
          day: {
            branch: 'reservation',
            state: {
              validTimeRange: defaultReservableTimeRange,
              reservation: {
                branch: 'timeRanges',
                state: [timeInputState('', '')]
              }
            }
          }
        }
      })
    })

    it('Common reservations for some children', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          r  r        | r  r  r  r  r        | r  r  r  r  r
      //          r  r        | r  r  r  r  r        | r  r  r  r  r
      //          _  _        | _  _  _  _  _        | _  _  _  _  _
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              reservations: [
                {
                  type: 'TIMES',
                  range: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                  staffCreated: false,
                  modifiedAt: HelsinkiDateTime.now(),
                  modifiedBy: user
                }
              ]
            },
            {
              ...emptyChild,
              childId: childId2,
              reservations: [
                {
                  type: 'TIMES',
                  range: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                  staffCreated: false,
                  modifiedAt: HelsinkiDateTime.now(),
                  modifiedBy: user
                }
              ]
            },
            { ...emptyChild, childId: childId3 }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      // Common reservations for child-1 and child-2
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'dailyTimes',
        state: {
          weekDayRange: [1, 5],
          day: {
            branch: 'reservation',
            state: {
              validTimeRange: defaultReservableTimeRange,
              reservation: {
                branch: 'timeRanges',
                state: [timeInputState('08:00', '16:00')]
              }
            }
          }
        }
      })

      // No common reservations for all children
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [childId1, childId2, childId3]
        })
      ).toEqual({
        branch: 'dailyTimes',
        state: {
          weekDayRange: [1, 5],
          day: {
            branch: 'reservation',
            state: {
              validTimeRange: defaultReservableTimeRange,
              reservation: {
                branch: 'timeRanges',
                state: [timeInputState('', '')]
              }
            }
          }
        }
      })
    })

    it('Reservations not required', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          n  n        | n  n  n  n  n        | n  n  n  n  n
      //          n  n        | n  n  n  n  n        | n  n  n  n  n
      //          n  n        | n  n  a  n  n        | n  n  a  n  n
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              scheduleType: 'FIXED_SCHEDULE'
            },
            {
              ...emptyChild,
              childId: childId2,
              scheduleType: 'FIXED_SCHEDULE'
            },
            {
              ...emptyChild,
              childId: childId3,
              scheduleType: 'FIXED_SCHEDULE',
              absence:
                day.date.getIsoDayOfWeek() === 3
                  ? {
                      type: 'OTHER_ABSENCE',
                      editable: true
                    }
                  : null
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      // Empty days for child-1 and child-2
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'dailyTimes',
        state: {
          weekDayRange: [1, 5],
          day: {
            branch: 'reservationNoTimes',
            state: 'present'
          }
        }
      })

      // Empty days for child-1 and child-2, child 3 has a single absence
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [childId1, childId2, childId3]
        })
      ).toEqual({
        branch: 'dailyTimes',
        state: {
          weekDayRange: [1, 5],
          day: {
            branch: 'reservationNoTimes',
            state: 'notSet'
          }
        }
      })
    })

    it('Term break', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          b  b        | b  b  b  b  n        | b  b  b  b  b
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              scheduleType: 'TERM_BREAK'
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      // Empty days for child-1 and child-2
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [childId1]
        })
      ).toEqual({
        branch: 'dailyTimes',
        state: {
          weekDayRange: [1, 5],
          day: {
            branch: 'readOnly',
            state: { type: 'termBreak' }
          }
        }
      })
    })

    it('Open holiday period covers the whole period', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          _H _H       | _H _H _H _H _H       | _H _H _H _H _H
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            { ...emptyChild, holidayPeriodEffect: { type: 'ReservationsOpen' } }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'dailyTimes',
        state: {
          weekDayRange: [1, 5],
          day: {
            branch: 'reservationNoTimes',
            state: 'notSet'
          }
        }
      })
    })

    it('Open holiday period covers part of the period => a normal reservation state', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          _H _H       | _H _H _H _H _H       | _H _  _  _  _
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              holidayPeriodEffect: day.date.isBefore(selectedRange.end)
                ? { type: 'ReservationsOpen' }
                : null
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'dailyTimes',
        state: {
          weekDayRange: [1, 5],
          day: {
            branch: 'reservation',
            state: {
              validTimeRange: defaultReservableTimeRange,
              reservation: {
                branch: 'timeRanges',
                state: [timeInputState('', '')]
              }
            }
          }
        }
      })
    })

    it('Open holiday period + absences for all children', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          aH aH       | aH aH aH aH aH       | aH aH aH aH aH
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: {
                type: 'OTHER_ABSENCE',
                editable: true
              }
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: {
                type: 'OTHER_ABSENCE',
                editable: true
              }
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'dailyTimes',
        state: {
          weekDayRange: [1, 5],
          day: {
            branch: 'reservationNoTimes',
            state: 'absent'
          }
        }
      })
    })

    it('Open holiday period + absences for some children', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          aH aH       | aH aH aH aH aH       | aH aH aH aH aH
      //          _H _H       | _H _H _H _H _H       | _H _H _H _H _H
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: {
                type: 'OTHER_ABSENCE',
                editable: true
              }
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: null
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'dailyTimes',
        state: {
          weekDayRange: [1, 5],
          day: {
            branch: 'reservationNoTimes',
            state: 'notSet'
          }
        }
      })
    })

    it('Open holiday period + reservations for all children', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          rH rH       | rH rH rH rH rH       | rH rH rH rH rH
      //          rH rH       | rH rH rH rH rH       | rH rH rH rH rH
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              reservations: [
                {
                  type: 'NO_TIMES',
                  staffCreated: false,
                  modifiedAt: HelsinkiDateTime.now(),
                  modifiedBy: user
                }
              ]
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              reservations: [
                {
                  type: 'NO_TIMES',
                  staffCreated: false,
                  modifiedAt: HelsinkiDateTime.now(),
                  modifiedBy: user
                }
              ]
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'dailyTimes',
        state: {
          weekDayRange: [1, 5],
          day: {
            branch: 'reservationNoTimes',
            state: 'present'
          }
        }
      })
    })

    it('Open holiday period + reservations for some children', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          rH rH       | rH rH rH rH rH       | rH rH rH rH rH
      //          _H _H       | _H _H _H _H _H       | _H _H _H _H _H
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              reservations: [
                {
                  type: 'NO_TIMES',
                  staffCreated: false,
                  modifiedAt: HelsinkiDateTime.now(),
                  modifiedBy: user
                }
              ]
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              reservations: []
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'DAILY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'dailyTimes',
        state: {
          weekDayRange: [1, 5],
          day: {
            branch: 'reservationNoTimes',
            state: 'notSet'
          }
        }
      })
    })
  })

  describe('WEEKLY', () => {
    it('No reservable days', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          [] []       | [] [] [] [] []       | [] [] [] [] []
      const dayProperties = new DayProperties(
        emptyCalendarDays,
        reservableRange
      )

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'notInitialized',
        state: undefined
      })
    })

    it('One reservable day', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          [] []       | [] [] [] [] []       | [] [] [] [] []
      const calendarDays = [...emptyCalendarDays]
      calendarDays[3] = {
        date: tuesday,
        holiday: false,
        children: [emptyChild]
      }

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [
          {
            weekDay: 2,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          }
        ]
      })
    })

    it('All weekdays are reservable', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          _  _        | _  _  _  _  _        | _  _  _  _  _
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [emptyChild]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [1, 2, 3, 4, 5].map((weekDay) => ({
          weekDay,
          day: {
            branch: 'reservation',
            state: {
              validTimeRange: defaultReservableTimeRange,
              reservation: {
                branch: 'timeRanges',
                state: [timeInputState('', '')]
              }
            }
          }
        }))
      })
    })

    it('All days are reservable', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          _  _  _  _  | _  _  _  _  _  _  _  | _  _  _  _  _
      const calendarDays: ReservationResponseDay[] =
        emptyCalendarDaysIncludingWeekend.map((day) => ({
          ...day,
          children: [emptyChild]
        }))

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [1, 2, 3, 4, 5, 6, 7].map((weekDay) => ({
          weekDay,
          day: {
            branch: 'reservation',
            state: {
              validTimeRange: defaultReservableTimeRange,
              reservation: {
                branch: 'timeRanges',
                state: [timeInputState('', '')]
              }
            }
          }
        }))
      })
    })

    it('Common absences', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          a  a        | a  a  a  a  a        | a  a  a  a  a
      //          a  a        | a  a  a  a  a        | a  a  a  a  a
      //          _  _        | _  _  a  _  _        | _  _  a  _  _
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              absence: { type: 'OTHER_ABSENCE', editable: true }
            },
            {
              ...emptyChild,
              childId: childId2,
              absence: { type: 'OTHER_ABSENCE', editable: true }
            },
            {
              ...emptyChild,
              childId: childId3,
              absence:
                day.date.getIsoDayOfWeek() === 3
                  ? { type: 'OTHER_ABSENCE', editable: true }
                  : null
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      // Common absence for child-1 and child-2
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [1, 2, 3, 4, 5].map((weekDay) => ({
          weekDay,
          day: {
            branch: 'reservation',
            state: {
              validTimeRange: defaultReservableTimeRange,
              reservation: {
                branch: 'absent',
                state: true
              }
            }
          }
        }))
      })

      // Common absence for child-1, child-2 and child-3 on one day
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2, childId3]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [
          {
            weekDay: 1,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            weekDay: 2,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            weekDay: 3,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                // this day has an absence for all children
                reservation: {
                  branch: 'absent',
                  state: true
                }
              }
            }
          },
          {
            weekDay: 4,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            weekDay: 5,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          }
        ]
      })
    })

    it('Common absences (marked by employee or free)', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          a  a        | a  a  a  a  a        | a  a  a  a  a
      //          a  a        | a  a  a  a  a        | a  a  a  a  a
      //          _  _        | _  _  a  _  _        | _  _  a  _  _
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              absence: { type: 'OTHER_ABSENCE', editable: false }
            },
            {
              ...emptyChild,
              childId: childId2,
              absence: { type: 'FREE_ABSENCE', editable: false }
            },
            {
              ...emptyChild,
              childId: childId3,
              absence:
                day.date.getIsoDayOfWeek() === 3
                  ? { type: 'OTHER_ABSENCE', editable: false }
                  : null
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      // Common absence for child-1 and child-2
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [1, 2, 3, 4, 5].map((weekDay) => ({
          weekDay,
          day: { branch: 'readOnly', state: { type: 'absentNotEditable' } }
        }))
      })

      // Common absence for child-1, child-2 and child-3 on one day
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2, childId3]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [
          {
            weekDay: 1,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            weekDay: 2,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            weekDay: 3,
            day: {
              // this day has an employee-marked absence for all children
              branch: 'readOnly',
              state: { type: 'absentNotEditable' }
            }
          },
          {
            weekDay: 4,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            weekDay: 5,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          }
        ]
      })
    })

    it('Common reservations', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          r1 r1       | r1 r1 r1 r1 r1       | r1 r1 r1 r1 r1
      //          r1 r1       | r1 r1 r1 r1 r1       | r1 r1 r1 r1 r1
      //          r2 r2       | r2 r2 r1 r2 r2       | r2 r2 r1 r2 r2
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              reservations: [
                {
                  type: 'TIMES',
                  range: new TimeRange(
                    LocalTime.of(8, day.date.getIsoDayOfWeek() * 5),
                    LocalTime.of(16, 0)
                  ),
                  staffCreated: false,
                  modifiedAt: HelsinkiDateTime.now(),
                  modifiedBy: user
                }
              ]
            },
            {
              ...emptyChild,
              childId: childId2,
              reservations: [
                {
                  type: 'TIMES',
                  range: new TimeRange(
                    LocalTime.of(8, day.date.getIsoDayOfWeek() * 5),
                    LocalTime.of(16, 0)
                  ),
                  staffCreated: false,
                  modifiedAt: HelsinkiDateTime.now(),
                  modifiedBy: user
                }
              ]
            },
            {
              ...emptyChild,
              childId: childId3,
              reservations: [
                {
                  type: 'TIMES',
                  range: new TimeRange(
                    LocalTime.of(8, 15),
                    LocalTime.of(16, 0)
                  ),
                  staffCreated: false,
                  modifiedAt: HelsinkiDateTime.now(),
                  modifiedBy: user
                }
              ]
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      // Common reservations for child-1 and child-2
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [
          {
            weekDay: 1,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('08:05', '16:00')]
                }
              }
            }
          },
          {
            weekDay: 2,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('08:10', '16:00')]
                }
              }
            }
          },
          {
            weekDay: 3,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('08:15', '16:00')]
                }
              }
            }
          },
          {
            weekDay: 4,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('08:20', '16:00')]
                }
              }
            }
          },
          {
            weekDay: 5,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('08:25', '16:00')]
                }
              }
            }
          }
        ]
      })

      // Common reservations for child-1, child-2 and child-3 on one day
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2, childId3]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [
          {
            weekDay: 1,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            weekDay: 2,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            weekDay: 3,
            day: {
              // This day has a common reservation for all children
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('08:15', '16:00')]
                }
              }
            }
          },
          {
            weekDay: 4,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            weekDay: 5,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          }
        ]
      })
    })

    it('Reservations not required', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          n  n        | n  n  n  a  n        | n  n  n  n  n
      //          n  n        | n  n  a  a  n        | n  n  a  n  n
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              scheduleType: 'FIXED_SCHEDULE',
              absence:
                day.date.getIsoDayOfWeek() === 4
                  ? { type: 'OTHER_ABSENCE', editable: true }
                  : null
            },
            {
              ...emptyChild,
              childId: childId2,
              scheduleType: 'FIXED_SCHEDULE',
              absence:
                day.date.getIsoDayOfWeek() === 3 ||
                day.date.getIsoDayOfWeek() === 4
                  ? { type: 'OTHER_ABSENCE', editable: true }
                  : null
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [
          {
            weekDay: 1,
            day: {
              branch: 'reservationNoTimes',
              state: 'present'
            }
          },
          {
            weekDay: 2,
            day: {
              branch: 'reservationNoTimes',
              state: 'present'
            }
          },
          {
            weekDay: 3,
            // The other child has an absence on this day
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 4,
            // Both children have an absence on this day
            day: {
              branch: 'reservationNoTimes',
              state: 'absent'
            }
          },
          {
            weekDay: 5,
            day: {
              branch: 'reservationNoTimes',
              state: 'present'
            }
          }
        ]
      })
    })

    it('Holidays', () => {
      // MO TU WE
      //    s  s
      //       s
      const calendarDays: ReservationResponseDay[] = [
        {
          date: monday,
          holiday: true,
          children: [
            { ...emptyChild, childId: childId1, shiftCare: false },
            { ...emptyChild, childId: childId2, shiftCare: false }
          ]
        },
        {
          date: tuesday,
          holiday: true,
          children: [
            { ...emptyChild, childId: childId1, shiftCare: true },
            { ...emptyChild, childId: childId2, shiftCare: false }
          ]
        },
        {
          date: wednesday,
          holiday: true,
          children: [
            { ...emptyChild, childId: childId1, shiftCare: true },
            { ...emptyChild, childId: childId2, shiftCare: true }
          ]
        }
      ]

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange: new FiniteDateRange(monday, wednesday),
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [
          {
            weekDay: 1,
            day: { branch: 'readOnly', state: { type: 'holiday' } }
          },
          {
            weekDay: 2,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            weekDay: 3,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          }
        ]
      })
    })

    it('Closed holiday period for one child, no holiday period effect for another child', () => {
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            { ...emptyChild, childId: childId1, holidayPeriodEffect: null },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsClosed' }
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [emptyChild.childId, childId2]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [1, 2, 3, 4, 5].map((weekDay) => ({
          weekDay,
          day: {
            branch: 'reservation',
            state: {
              validTimeRange: defaultReservableTimeRange,
              reservation: {
                branch: 'timeRanges',
                state: [timeInputState('', '')]
              }
            }
          }
        }))
      })
    })

    it('Open holiday period covers the whole period', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          _H _H       | _H _H _H _H _H       | _H _H _H _H _H
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            { ...emptyChild, holidayPeriodEffect: { type: 'ReservationsOpen' } }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [1, 2, 3, 4, 5].map((weekDay) => ({
          weekDay,
          day: {
            branch: 'reservationNoTimes',
            state: 'notSet'
          }
        }))
      })
    })

    it('Open holiday period covers part of the period => a normal reservation state for non-holiday period days', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          _H _H       | _H _H _H _H _H       | _H _  _  _  _
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              holidayPeriodEffect: day.date.isBefore(selectedRange.end)
                ? { type: 'ReservationsOpen' }
                : null
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [
          {
            weekDay: 1,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 2,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            weekDay: 3,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 4,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 5,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          }
        ]
      })
    })

    it('Open holiday period + common absences', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          aH aH       | aH aH aH aH aH       | aH aH aH aH aH
      //          aH aH       | aH aH aH aH aH       | aH aH aH aH aH
      //          _H _H       | _H _H aH _H _H       | _H _H aH _H _H
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: { type: 'OTHER_ABSENCE', editable: true }
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: { type: 'OTHER_ABSENCE', editable: true }
            },
            {
              ...emptyChild,
              childId: childId3,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence:
                day.date.getIsoDayOfWeek() === 3
                  ? { type: 'OTHER_ABSENCE', editable: true }
                  : null
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      // Common absences for child-1 and child-2
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [1, 2, 3, 4, 5].map((weekDay) => ({
          weekDay,
          day: {
            branch: 'reservationNoTimes',
            state: 'absent'
          }
        }))
      })

      // Common absences for child-1, child-2 and child-3 on one day
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2, childId3]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [
          {
            weekDay: 1,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 2,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 3,
            day: {
              // This day has an absence for all children
              branch: 'reservationNoTimes',
              state: 'absent'
            }
          },
          {
            weekDay: 4,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 5,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          }
        ]
      })
    })

    it('Open holiday period + common absences (marked by employee)', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          aH aH       | aH aH aH aH aH       | aH aH aH aH aH
      //          aH aH       | aH aH aH aH aH       | aH aH aH aH aH
      //          _H _H       | _H _H aH _H _H       | _H _H aH _H _H
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: { type: 'OTHER_ABSENCE', editable: false }
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: { type: 'OTHER_ABSENCE', editable: false }
            },
            {
              ...emptyChild,
              childId: childId3,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence:
                day.date.getIsoDayOfWeek() === 3
                  ? { type: 'OTHER_ABSENCE', editable: false }
                  : null
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      // Common absences for child-1 and child-2
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [1, 2, 3, 4, 5].map((weekDay) => ({
          weekDay,
          day: { branch: 'readOnly', state: { type: 'absentNotEditable' } }
        }))
      })

      // Common absences for child-1, child-2 and child-3 on one day
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2, childId3]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [
          {
            weekDay: 1,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 2,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 3,
            day: {
              // This day has an employee-marked absence for all children
              branch: 'readOnly',
              state: { type: 'absentNotEditable' }
            }
          },
          {
            weekDay: 4,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 5,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          }
        ]
      })
    })

    it('Open holiday period + common reservations', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          rH rH       | rH rH rH rH rH       | rH rH rH rH rH
      //          rH rH       | rH rH rH rH rH       | rH rH rH rH rH
      //          _H _H       | _H _H rH _H _H       | _H _H rH _H _H
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              reservations: [
                {
                  type: 'NO_TIMES',
                  staffCreated: false,
                  modifiedAt: HelsinkiDateTime.now(),
                  modifiedBy: user
                }
              ]
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              reservations: [
                {
                  type: 'NO_TIMES',
                  staffCreated: false,
                  modifiedAt: HelsinkiDateTime.now(),
                  modifiedBy: user
                }
              ]
            },
            {
              ...emptyChild,
              childId: childId3,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              reservations:
                day.date.getIsoDayOfWeek() === 3
                  ? [
                      {
                        type: 'TIMES',
                        range: new TimeRange(
                          LocalTime.of(8, 15),
                          LocalTime.of(16, 0)
                        ),
                        staffCreated: false,
                        modifiedAt: HelsinkiDateTime.now(),
                        modifiedBy: user
                      }
                    ]
                  : []
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      // Common reservations for child-1 and child-2
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [1, 2, 3, 4, 5].map((weekDay) => ({
          weekDay,
          day: {
            branch: 'reservationNoTimes',
            state: 'present'
          }
        }))
      })

      // Common reservations for child-1, child-2 and child-3 on one day
      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2, childId3]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [
          {
            weekDay: 1,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 2,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 3,
            // This day has a reservation for all children
            day: {
              branch: 'reservationNoTimes',
              state: 'present'
            }
          },
          {
            weekDay: 4,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 5,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          }
        ]
      })
    })

    it('Open holiday period + mixed requiresReservation', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          rH rH       | _H _H aH rH rH       | _H _H aH rH rH
      //          nH nH       | _H nH aH nH nH       | nH nH aH nH nH
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              scheduleType: 'RESERVATION_REQUIRED',
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              reservations:
                day.date.getIsoDayOfWeek() === 4
                  ? [
                      {
                        type: 'NO_TIMES',
                        staffCreated: false,
                        modifiedAt: HelsinkiDateTime.now(),
                        modifiedBy: user
                      }
                    ]
                  : day.date.getIsoDayOfWeek() === 5
                    ? [
                        {
                          type: 'TIMES',
                          range: new TimeRange(
                            LocalTime.of(8, 0),
                            LocalTime.of(16, 0)
                          ),
                          staffCreated: false,
                          modifiedAt: HelsinkiDateTime.now(),
                          modifiedBy: user
                        }
                      ]
                    : [],
              absence:
                day.date.getIsoDayOfWeek() === 3
                  ? { type: 'OTHER_ABSENCE', editable: true }
                  : null
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              scheduleType: 'FIXED_SCHEDULE',
              absence:
                day.date.getIsoDayOfWeek() === 3
                  ? { type: 'OTHER_ABSENCE', editable: true }
                  : null
            }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [
          {
            weekDay: 1,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 2,
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            weekDay: 3,
            day: {
              branch: 'reservationNoTimes',
              state: 'absent'
            }
          },
          {
            weekDay: 4,
            day: {
              branch: 'reservationNoTimes',
              state: 'present'
            }
          },
          {
            weekDay: 5,
            day: {
              branch: 'reservationNoTimes',
              state: 'present'
            }
          }
        ]
      })
    })

    it('Intersection of normal reservable time ranges is used for validating reservation times', () => {
      const calendarDays = emptyCalendarDays.map((day) => ({
        ...day,
        children: [
          {
            ...emptyChild,
            childId: childId1,
            reservableTimeRange: {
              type: 'NORMAL' as const,
              range: new TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0))
            }
          },
          {
            ...emptyChild,
            childId: childId2,
            reservableTimeRange: {
              type: 'NORMAL' as const,
              range: new TimeRange(LocalTime.of(7, 0), LocalTime.of(16, 0))
            }
          },
          {
            ...emptyChild,
            childId: childId3,
            reservableTimeRange: {
              type: 'INTERMITTENT_SHIFT_CARE' as const,
              // Very short operation time -> can still reserve any times
              placementUnitOperationTime: new TimeRange(
                LocalTime.of(10, 0),
                LocalTime.of(13, 0)
              )
            }
          },
          {
            ...emptyChild,
            childId: childId4,
            reservableTimeRange: {
              type: 'INTERMITTENT_SHIFT_CARE' as const,
              // Placement unit not open at all -> can still reserve any times
              placementUnitOperationTime: null
            }
          }
        ]
      }))

      // Intersection of the two normal reservable time ranges
      const expectedValidRange = new TimeRange(
        LocalTime.of(8, 0),
        LocalTime.of(16, 0)
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'WEEKLY',
          selectedRange,
          selectedChildren: [childId1, childId2, childId3, childId4]
        })
      ).toEqual({
        branch: 'weeklyTimes',
        state: [1, 2, 3, 4, 5].map((weekDay) => ({
          weekDay,
          day: {
            branch: 'reservation',
            state: {
              validTimeRange: expectedValidRange,
              reservation: {
                branch: 'timeRanges',
                state: [timeInputState('', '', expectedValidRange)]
              }
            }
          }
        }))
      })
    })
  })

  describe('IRREGULAR', () => {
    const selectedRangeWeekDays = [...selectedRange.dates()].filter(
      (date) => !date.isWeekend()
    )

    it('No reservable days', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          [] []       | [] [] [] [] []       | [] [] [] [] []
      const dayProperties = new DayProperties(
        emptyCalendarDays,
        reservableRange
      )

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'IRREGULAR',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'notInitialized',
        state: undefined
      })
    })

    it('One reservable day', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          [] []       | [] _  [] [] []       | [] [] [] [] []
      const calendarDays = [...emptyCalendarDays]
      calendarDays[3] = {
        date: tuesday,
        holiday: false,
        children: [emptyChild]
      }

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'IRREGULAR',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'irregularTimes',
        state: selectedRangeWeekDays.map((date) => ({
          date,
          day: date.isEqual(tuesday)
            ? {
                branch: 'reservation',
                state: {
                  validTimeRange: defaultReservableTimeRange,
                  reservation: {
                    branch: 'timeRanges',
                    state: [timeInputState('', '')]
                  }
                }
              }
            : { branch: 'readOnly', state: { type: 'noChildren' } }
        }))
      })
    })

    it('All weekdays are reservable', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          _  _        | _  _  _  _  _        | _  _  _  _  _
      const calendarDays = emptyCalendarDays.map((day) => ({
        ...day,
        children: [emptyChild]
      }))

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'IRREGULAR',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'irregularTimes',
        state: selectedRangeWeekDays.map((date) => ({
          date,
          day: {
            branch: 'reservation',
            state: {
              validTimeRange: defaultReservableTimeRange,
              reservation: {
                branch: 'timeRanges',
                state: [timeInputState('', '')]
              }
            }
          }
        }))
      })
    })

    it('All days are reservable', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          _  _  _  _  | _  _  _  _  _  _  _  | _  _  _  _  _
      const calendarDays = emptyCalendarDaysIncludingWeekend.map((day) => ({
        ...day,
        children: [emptyChild]
      }))

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'IRREGULAR',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'irregularTimes',
        state: [...selectedRange.dates()].map((date) => ({
          date,
          day: {
            branch: 'reservation',
            state: {
              validTimeRange: defaultReservableTimeRange,
              reservation: {
                branch: 'timeRanges',
                state: [timeInputState('', '')]
              }
            }
          }
        }))
      })
    })

    it('Reservations + absences + employee-marked absences', () => {
      const range = new FiniteDateRange(monday, fridayNextWeek)
      const rangeWeekDays = [...range.dates()].filter(
        (date) => !date.isWeekend()
      )

      const r: ReservationResponse = {
        type: 'TIMES',
        range: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
        staffCreated: false,
        modifiedAt: HelsinkiDateTime.now(),
        modifiedBy: user
      }
      const ae: AbsenceInfo = { type: 'OTHER_ABSENCE', editable: true }
      const an: AbsenceInfo = { type: 'OTHER_ABSENCE', editable: false }
      // n = no reservation required
      // nae = n + ae

      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU WE  TH  fr sa su
      //                      | r  r  r  ae ae       | an _  nae nae n
      //                      | r  _  ae ae _        | an an nae n   n
      const calendarDays: ReservationResponseDay[] = [
        {
          date: rangeWeekDays[0],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              reservations: [r]
            },
            {
              ...emptyChild,
              childId: childId2,
              reservations: [r]
            }
          ]
        },
        {
          date: rangeWeekDays[1],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              reservations: [r]
            },
            {
              ...emptyChild,
              childId: childId2
            }
          ]
        },
        {
          date: rangeWeekDays[2],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              reservations: [r]
            },
            {
              ...emptyChild,
              childId: childId2,
              absence: ae
            }
          ]
        },
        {
          date: rangeWeekDays[3],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              absence: ae
            },
            {
              ...emptyChild,
              childId: childId2,
              absence: ae
            }
          ]
        },
        {
          date: rangeWeekDays[4],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              absence: ae
            },
            {
              ...emptyChild,
              childId: childId2
            }
          ]
        },
        {
          date: rangeWeekDays[5],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              absence: an
            },
            {
              ...emptyChild,
              childId: childId2,
              absence: an
            }
          ]
        },
        {
          date: rangeWeekDays[6],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1
            },
            {
              ...emptyChild,
              childId: childId2,
              absence: an
            }
          ]
        },
        {
          date: rangeWeekDays[7],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              scheduleType: 'FIXED_SCHEDULE',
              absence: ae
            },
            {
              ...emptyChild,
              childId: childId2,
              scheduleType: 'FIXED_SCHEDULE',
              absence: ae
            }
          ]
        },
        {
          date: rangeWeekDays[8],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              scheduleType: 'FIXED_SCHEDULE',
              absence: ae
            },
            {
              ...emptyChild,
              childId: childId2,
              scheduleType: 'FIXED_SCHEDULE'
            }
          ]
        },
        {
          date: rangeWeekDays[9],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              scheduleType: 'FIXED_SCHEDULE'
            },
            {
              ...emptyChild,
              childId: childId2,
              scheduleType: 'FIXED_SCHEDULE'
            }
          ]
        }
      ]

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'IRREGULAR',
          selectedRange: range,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'irregularTimes',
        state: [
          {
            date: rangeWeekDays[0],
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('08:00', '16:00')]
                }
              }
            }
          },
          {
            date: rangeWeekDays[1],
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            date: rangeWeekDays[2],
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            date: rangeWeekDays[3],
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'absent',
                  state: true
                }
              }
            }
          },
          {
            date: rangeWeekDays[4],
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            date: rangeWeekDays[5],
            day: { branch: 'readOnly', state: { type: 'absentNotEditable' } }
          },
          {
            date: rangeWeekDays[6],
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            date: rangeWeekDays[7],
            day: {
              branch: 'reservationNoTimes',
              state: 'absent'
            }
          },
          {
            date: rangeWeekDays[8],
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            date: rangeWeekDays[9],
            day: {
              branch: 'reservationNoTimes',
              state: 'present'
            }
          }
        ]
      })
    })

    it('Reservation + absence', () => {
      const range = new FiniteDateRange(monday, monday)

      const calendarDays: ReservationResponseDay[] = [
        {
          date: monday,
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              reservations: [
                {
                  type: 'TIMES',
                  range: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                  staffCreated: false,
                  modifiedAt: HelsinkiDateTime.now(),
                  modifiedBy: user
                }
              ],
              absence: { type: 'OTHER_ABSENCE', editable: true }
            }
          ]
        }
      ]

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'IRREGULAR',
          selectedRange: range,
          selectedChildren: [childId1]
        })
      ).toEqual({
        branch: 'irregularTimes',
        state: [
          {
            date: monday,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('08:00', '16:00')]
                }
              }
            }
          }
        ]
      })
    })

    it('Reservation + absence marked by employee', () => {
      const range = new FiniteDateRange(monday, monday)

      const calendarDays: ReservationResponseDay[] = [
        {
          date: monday,
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              reservations: [
                {
                  type: 'TIMES',
                  range: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
                  staffCreated: false,
                  modifiedAt: HelsinkiDateTime.now(),
                  modifiedBy: user
                }
              ],
              absence: { type: 'OTHER_ABSENCE', editable: false }
            }
          ]
        }
      ]

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'IRREGULAR',
          selectedRange: range,
          selectedChildren: [childId1]
        })
      ).toEqual({
        branch: 'irregularTimes',
        state: [
          {
            date: monday,
            day: { branch: 'readOnly', state: { type: 'absentNotEditable' } }
          }
        ]
      })
    })

    it('Holidays', () => {
      // MO TU WE
      //    s  s
      //       s

      // Children without shift care are excluded from the day's children array
      const calendarDays: ReservationResponseDay[] = [
        {
          date: monday,
          holiday: true,
          children: []
        },
        {
          date: tuesday,
          holiday: true,
          children: [{ ...emptyChild, childId: childId1, shiftCare: true }]
        },
        {
          date: wednesday,
          holiday: true,
          children: [
            { ...emptyChild, childId: childId1, shiftCare: true },
            { ...emptyChild, childId: childId2, shiftCare: true }
          ]
        }
      ]

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'IRREGULAR',
          selectedRange: new FiniteDateRange(monday, wednesday),
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'irregularTimes',
        state: [
          {
            date: monday,
            day: { branch: 'readOnly', state: { type: 'holiday' } }
          },
          {
            date: tuesday,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            date: wednesday,
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          }
        ]
      })
    })

    it('Open holiday period + all weekdays are reservable', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          _H _H       | _H _H _H _H _H       | _H _H _H _H _H
      const calendarDays: ReservationResponseDay[] = emptyCalendarDays.map(
        (day) => ({
          ...day,
          children: [
            { ...emptyChild, holidayPeriodEffect: { type: 'ReservationsOpen' } }
          ]
        })
      )

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'IRREGULAR',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'irregularTimes',
        state: selectedRangeWeekDays.map((date) => ({
          date,
          day: {
            branch: 'reservationNoTimes',
            state: 'notSet'
          }
        }))
      })
    })

    it('Open holiday period + reservations + absences + employee-marked absences', () => {
      const r: ReservationResponse = {
        type: 'NO_TIMES',
        staffCreated: false,
        modifiedAt: HelsinkiDateTime.now(),
        modifiedBy: user
      }
      const aa: AbsenceInfo = { type: 'OTHER_ABSENCE', editable: true }
      const ae: AbsenceInfo = { type: 'OTHER_ABSENCE', editable: false }

      // mo tu we th fr sa su | MO TU WE  TH  FR  SA SU | MO  TU  we th fr sa su
      //                      | rH rH rH  aaH aaH       | aeH _ H
      //                      | rH _H aaH aaH _ H       | aeH aeH
      const calendarDays: ReservationResponseDay[] = [
        {
          date: selectedRangeWeekDays[0],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              reservations: [r]
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              reservations: [r]
            }
          ]
        },
        {
          date: selectedRangeWeekDays[1],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              reservations: [r]
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' }
            }
          ]
        },
        {
          date: selectedRangeWeekDays[2],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              reservations: [r]
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: aa
            }
          ]
        },
        {
          date: selectedRangeWeekDays[3],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: aa
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: aa
            }
          ]
        },
        {
          date: selectedRangeWeekDays[4],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: aa
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' }
            }
          ]
        },
        {
          date: selectedRangeWeekDays[5],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: ae
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: ae
            }
          ]
        },
        {
          date: selectedRangeWeekDays[6],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsOpen' }
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsOpen' },
              absence: ae
            }
          ]
        }
      ]

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'IRREGULAR',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'irregularTimes',
        state: [
          {
            date: selectedRangeWeekDays[0],
            day: {
              branch: 'reservationNoTimes',
              state: 'present'
            }
          },
          {
            date: selectedRangeWeekDays[1],
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            date: selectedRangeWeekDays[2],
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            date: selectedRangeWeekDays[3],
            day: {
              branch: 'reservationNoTimes',
              state: 'absent'
            }
          },
          {
            date: selectedRangeWeekDays[4],
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          },
          {
            date: selectedRangeWeekDays[5],
            day: { branch: 'readOnly', state: { type: 'absentNotEditable' } }
          },
          {
            date: selectedRangeWeekDays[6],
            day: {
              branch: 'reservationNoTimes',
              state: 'notSet'
            }
          }
        ]
      })
    })

    it('Closed holiday period + reservations + absences + employee-marked absences', () => {
      const r: ReservationResponse = {
        type: 'NO_TIMES',
        staffCreated: false,
        modifiedAt: HelsinkiDateTime.now(),
        modifiedBy: user
      }
      const aa: AbsenceInfo = { type: 'OTHER_ABSENCE', editable: true }
      const ae: AbsenceInfo = { type: 'OTHER_ABSENCE', editable: false }

      // mo tu we th fr sa su | MO TU WE  TH  FR  SA SU | MO  TU  we th fr sa su
      //                      | rh _h r h aah aah       | aeh _ h
      //                      | rh _h aah aah _ h       | aeh aeh
      const calendarDays: ReservationResponseDay[] = [
        {
          date: selectedRangeWeekDays[0],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsClosed' },
              reservations: [r]
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsClosed' },
              reservations: [r]
            }
          ]
        },
        {
          date: selectedRangeWeekDays[1],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsClosed' }
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsClosed' }
            }
          ]
        },
        {
          date: selectedRangeWeekDays[2],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsClosed' },
              reservations: [r]
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsClosed' },
              absence: aa
            }
          ]
        },
        {
          date: selectedRangeWeekDays[3],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsClosed' },
              absence: aa
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsClosed' },
              absence: aa
            }
          ]
        },
        {
          date: selectedRangeWeekDays[4],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsClosed' },
              absence: aa
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsClosed' }
            }
          ]
        },
        {
          date: selectedRangeWeekDays[5],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsClosed' },
              absence: ae
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsClosed' },
              absence: ae
            }
          ]
        },
        {
          date: selectedRangeWeekDays[6],
          holiday: false,
          children: [
            {
              ...emptyChild,
              childId: childId1,
              holidayPeriodEffect: { type: 'ReservationsClosed' }
            },
            {
              ...emptyChild,
              childId: childId2,
              holidayPeriodEffect: { type: 'ReservationsClosed' },
              absence: ae
            }
          ]
        }
      ]

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'IRREGULAR',
          selectedRange,
          selectedChildren: [childId1, childId2]
        })
      ).toEqual({
        branch: 'irregularTimes',
        state: [
          {
            date: selectedRangeWeekDays[0],
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            date: selectedRangeWeekDays[1],
            day: { branch: 'readOnly', state: { type: 'reservationClosed' } }
          },
          {
            date: selectedRangeWeekDays[2],
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            date: selectedRangeWeekDays[3],
            day: { branch: 'readOnly', state: { type: 'reservationClosed' } }
          },
          {
            date: selectedRangeWeekDays[4],
            day: { branch: 'readOnly', state: { type: 'reservationClosed' } }
          },
          {
            date: selectedRangeWeekDays[5],
            day: { branch: 'readOnly', state: { type: 'absentNotEditable' } }
          },
          {
            date: selectedRangeWeekDays[6],
            day: { branch: 'readOnly', state: { type: 'reservationClosed' } }
          }
        ]
      })
    })

    it('Closed holiday period + reservation not required + absences', () => {
      const aa: AbsenceInfo = { type: 'OTHER_ABSENCE', editable: true }
      const ae: AbsenceInfo = { type: 'OTHER_ABSENCE', editable: false }

      const selectedRange = new FiniteDateRange(monday, wednesday)

      // mo tu we th fr sa su | MO  TU  WE th fr sa su | mo tu we th fr sa su
      //                      | aah aeh _h              |
      const calendarDays: ReservationResponseDay[] = [
        {
          date: selectedRangeWeekDays[0],
          holiday: false,
          children: [
            {
              ...emptyChild,
              scheduleType: 'FIXED_SCHEDULE',
              holidayPeriodEffect: { type: 'ReservationsClosed' },
              absence: aa
            }
          ]
        },
        {
          date: selectedRangeWeekDays[1],
          holiday: false,
          children: [
            {
              ...emptyChild,
              scheduleType: 'FIXED_SCHEDULE',
              holidayPeriodEffect: { type: 'ReservationsClosed' },
              absence: ae
            }
          ]
        },
        {
          date: selectedRangeWeekDays[2],
          holiday: false,
          children: [
            {
              ...emptyChild,
              scheduleType: 'FIXED_SCHEDULE',
              holidayPeriodEffect: { type: 'ReservationsClosed' }
            }
          ]
        }
      ]

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(dayProperties, undefined, {
          repetition: 'IRREGULAR',
          selectedRange,
          selectedChildren: [emptyChild.childId]
        })
      ).toEqual({
        branch: 'irregularTimes',
        state: [
          {
            date: selectedRangeWeekDays[0],
            day: { branch: 'readOnly', state: { type: 'reservationClosed' } }
          },
          {
            date: selectedRangeWeekDays[1],
            day: { branch: 'readOnly', state: { type: 'absentNotEditable' } }
          },
          {
            date: selectedRangeWeekDays[2],
            day: {
              branch: 'reservationNoTimes',
              state: 'present'
            }
          }
        ]
      })
    })

    it('Previous days in state are reused when children stay the same', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          _  _        | _  _  _  _  _        | _  _  _  _  _
      const calendarDays = emptyCalendarDays.map((day) => ({
        ...day,
        children: [emptyChild]
      }))

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(
          dayProperties,
          {
            childrenChanged: false,
            times: {
              branch: 'irregularTimes',
              state: [
                {
                  date: selectedRangeWeekDays[1],
                  day: {
                    branch: 'reservation',
                    state: {
                      validTimeRange: defaultReservableTimeRange,
                      reservation: {
                        branch: 'timeRanges',
                        state: [
                          {
                            value: { startTime: '08:15', endTime: '13:45' },
                            validRange: defaultReservableTimeRange
                          }
                        ]
                      }
                    }
                  }
                }
              ]
            }
          },
          {
            repetition: 'IRREGULAR',
            selectedRange: new FiniteDateRange(
              selectedRangeWeekDays[0],
              selectedRangeWeekDays[2]
            ),
            selectedChildren: [emptyChild.childId]
          }
        )
      ).toEqual({
        branch: 'irregularTimes',
        state: [
          {
            date: selectedRangeWeekDays[0],
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            date: selectedRangeWeekDays[1],
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('08:15', '13:45')]
                }
              }
            }
          },
          {
            date: selectedRangeWeekDays[2],
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          }
        ]
      })
    })

    it('Previous days in state are NOT reused when children change', () => {
      // mo tu we th fr sa su | MO TU WE TH FR SA SU | MO TU we th fr sa su
      //          _  _        | _  _  _  _  _        | _  _  _  _  _
      const calendarDays = emptyCalendarDays.map((day) => ({
        ...day,
        children: [emptyChild]
      }))

      const dayProperties = new DayProperties(calendarDays, reservableRange)

      expect(
        resetTimes(
          dayProperties,
          {
            childrenChanged: true,
            times: {
              branch: 'irregularTimes',
              state: [
                {
                  date: selectedRangeWeekDays[1],
                  day: {
                    branch: 'reservation',
                    state: {
                      validTimeRange: defaultReservableTimeRange,
                      reservation: {
                        branch: 'timeRanges',
                        state: [
                          {
                            value: { startTime: '08:15', endTime: '13:45' },
                            validRange: defaultReservableTimeRange
                          }
                        ]
                      }
                    }
                  }
                }
              ]
            }
          },
          {
            repetition: 'IRREGULAR',
            selectedRange: new FiniteDateRange(
              selectedRangeWeekDays[0],
              selectedRangeWeekDays[2]
            ),
            selectedChildren: [emptyChild.childId]
          }
        )
      ).toEqual({
        branch: 'irregularTimes',
        state: [
          {
            date: selectedRangeWeekDays[0],
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            date: selectedRangeWeekDays[1],
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          },
          {
            date: selectedRangeWeekDays[2],
            day: {
              branch: 'reservation',
              state: {
                validTimeRange: defaultReservableTimeRange,
                reservation: {
                  branch: 'timeRanges',
                  state: [timeInputState('', '')]
                }
              }
            }
          }
        ]
      })
    })
  })
})
