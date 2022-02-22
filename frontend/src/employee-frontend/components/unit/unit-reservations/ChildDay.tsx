// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { css } from 'styled-components'

import {
  DailyServiceTimes,
  getTimesOnWeekday,
  isIrregular,
  isRegular,
  isVariableTime,
  TimeRange as ServiceTimesTimeRange
} from 'lib-common/api-types/child/common'
import {
  ChildRecordOfDay,
  OperationalDay
} from 'lib-common/api-types/reservations'
import { TimeRange } from 'lib-common/generated/api-types/reservations'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { attendanceTimeDiffers } from 'lib-common/reservations'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'

import { useTranslation } from '../../../state/i18n'

import AbsenceDay from './AbsenceDay'
import { EditState, TimeRangeWithErrors } from './reservation-table-edit-state'

type ReservationOrServiceTime =
  | (ServiceTimesTimeRange & { type: 'reservation' | 'service-time' })
  | undefined
const getReservationOrServiceTimeOfDay = (
  reservation: TimeRange | null,
  serviceTimeOfDay: ServiceTimesTimeRange | null
): ReservationOrServiceTime =>
  reservation
    ? {
        start: reservation.startTime,
        end: reservation.endTime,
        type: 'reservation'
      }
    : serviceTimeOfDay
    ? { ...serviceTimeOfDay, type: 'service-time' }
    : undefined

interface Props {
  day: OperationalDay
  dailyServiceTimes: DailyServiceTimes | null
  dataForAllDays: Record<JsonOf<LocalDate>, ChildRecordOfDay>
  rowIndex: number
  editState?: EditState
  deleteAbsence: (i: number, d: LocalDate) => void
  updateReservation: (i: number, d: LocalDate, r: TimeRange) => void
  saveReservation: (d: LocalDate) => void
  updateAttendance: (i: number, d: LocalDate, r: TimeRange) => void
  saveAttendance: (d: LocalDate) => void
}

export default React.memo(function ChildDay({
  day,
  dailyServiceTimes,
  dataForAllDays,
  rowIndex,
  editState,
  deleteAbsence,
  updateReservation,
  saveReservation,
  updateAttendance,
  saveAttendance
}: Props) {
  const { i18n } = useTranslation()

  const dailyData = dataForAllDays[day.date.formatIso()]

  if (!dailyData) return null

  if (day.isHoliday && !dailyData.reservation && !dailyData.attendance)
    return null

  const serviceTimesAvailable =
    dailyServiceTimes !== null && !isVariableTime(dailyServiceTimes)
  const serviceTimeOfDay =
    dailyServiceTimes === null || isVariableTime(dailyServiceTimes)
      ? null
      : isRegular(dailyServiceTimes)
      ? dailyServiceTimes.regularTimes
      : isIrregular(dailyServiceTimes)
      ? getTimesOnWeekday(dailyServiceTimes, day.date.getIsoDayOfWeek())
      : null

  const expectedTimeForThisDay = getReservationOrServiceTimeOfDay(
    dailyData.reservation,
    serviceTimeOfDay
  )
  const maybeServiceTimeIndicator =
    expectedTimeForThisDay?.type === 'service-time' &&
    ` ${i18n.unit.attendanceReservations.serviceTimeIndicator}`

  const absence = editState
    ? editState.absences[rowIndex][day.date.formatIso()]
    : dailyData.absence

  return (
    <DateCell>
      <TimesRow data-qa={`reservation-${day.date.formatIso()}-${rowIndex}`}>
        {absence ? (
          <AbsenceDay
            type={absence.type}
            onDelete={
              editState ? () => deleteAbsence(rowIndex, day.date) : undefined
            }
          />
        ) : editState ? (
          <TimeRangeEditor
            timeRange={editState.reservations[rowIndex][day.date.formatIso()]}
            update={(timeRange) =>
              updateReservation(rowIndex, day.date, timeRange)
            }
            save={() => saveReservation(day.date)}
          />
        ) : expectedTimeForThisDay ? (
          /* show actual reservation or service time if exists */
          <>
            <ReservationTime data-qa="reservation-start">
              {expectedTimeForThisDay.start}
              {maybeServiceTimeIndicator}
            </ReservationTime>
            <ReservationTime data-qa="reservation-end">
              {expectedTimeForThisDay.end}
              {maybeServiceTimeIndicator}
            </ReservationTime>
          </>
        ) : serviceTimesAvailable && serviceTimeOfDay === null ? (
          /* else if daily service times are known but there is none for this day of week, show day off */
          <ReservationTime data-qa="reservation-day-off">
            {i18n.unit.attendanceReservations.dayOff}
          </ReservationTime>
        ) : (
          /* else show missing service time */
          <ReservationTime warning data-qa="reservation-missing">
            {i18n.unit.attendanceReservations.missingServiceTime}
          </ReservationTime>
        )}
      </TimesRow>
      <TimesRow data-qa={`attendance-${day.date.formatIso()}-${rowIndex}`}>
        {editState && day.date.isEqualOrBefore(LocalDate.today()) ? (
          <TimeRangeEditor
            timeRange={editState.attendances[rowIndex][day.date.formatIso()]}
            update={(timeRange) =>
              updateAttendance(rowIndex, day.date, timeRange)
            }
            save={() => saveAttendance(day.date)}
          />
        ) : (
          <>
            <AttendanceTime
              data-qa="attendance-start"
              warning={attendanceTimeDiffers(
                expectedTimeForThisDay?.start,
                dailyData.attendance?.startTime
              )}
            >
              {dailyData.attendance?.startTime ?? '–'}
            </AttendanceTime>
            <AttendanceTime
              data-qa="attendance-end"
              warning={attendanceTimeDiffers(
                dailyData.attendance?.endTime,
                expectedTimeForThisDay?.end
              )}
            >
              {dailyData.attendance?.endTime ?? '–'}
            </AttendanceTime>
          </>
        )}
      </TimesRow>
    </DateCell>
  )
})

const TimeRangeEditor = React.memo(function TimeRangeEditor({
  timeRange,
  update,
  save
}: {
  timeRange: TimeRangeWithErrors
  update: (v: TimeRange) => void
  save: () => void
}) {
  const { startTime, endTime, errors } = timeRange

  return (
    <>
      <TimeInputWithoutPadding
        value={startTime}
        onChange={(value) => update({ startTime: value, endTime })}
        onBlur={save}
        info={errors.startTime ? { status: 'warning', text: '' } : undefined}
        data-qa="input-start-time"
      />
      <TimeInputWithoutPadding
        value={endTime}
        onChange={(value) => update({ startTime, endTime: value })}
        onBlur={save}
        info={errors.endTime ? { status: 'warning', text: '' } : undefined}
        data-qa="input-end-time"
      />
    </>
  )
})

const TimeInputWithoutPadding = styled(TimeInput)`
  padding: 0;
  width: calc(2.8em);
  max-width: calc(2.8em);
  background: transparent;

  &:focus {
    padding: 0;
  }
`

const DateCell = styled.div`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
`

const TimesRow = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-evenly;
  padding: ${defaultMargins.xs};
  gap: ${defaultMargins.xs};

  :nth-child(even) {
    background: ${colors.grayscale.g4};
  }
`

const TimeCell = styled.div<{ warning?: boolean }>`
  flex: 1 0 54px;
  text-align: center;
  white-space: nowrap;
  ${(p) =>
    p.warning &&
    css`
      color: ${colors.accents.a2orangeDark};
    `};
`

const AttendanceTime = styled(TimeCell)`
  font-weight: ${fontWeights.semibold};
`

const ReservationTime = styled(TimeCell)``
