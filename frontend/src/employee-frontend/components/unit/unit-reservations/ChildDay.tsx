// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useCallback } from 'react'
import styled, { css } from 'styled-components'

import {
  getTimesOnWeekday,
  isIrregular,
  isRegular,
  isVariableTime
} from 'lib-common/api-types/daily-service-times'
import type {
  ChildRecordOfDay,
  OperationalDay
} from 'lib-common/api-types/reservations'
import type { Reservation } from 'lib-common/generated/api-types/reservations'
import type { TimeRange as ServiceTimesTimeRange } from 'lib-common/generated/api-types/shared'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import type { TimeRange } from 'lib-common/reservations'
import { attendanceTimeDiffers } from 'lib-common/reservations'
import { fontWeights, Light } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'

import { useTranslation } from '../../../state/i18n'

import AbsenceDay from './AbsenceDay'
import { TimeRangeEditor } from './attendance-elements'
import type { EditState } from './reservation-table-edit-state'

function getExpectedAttendanceTime(
  reservation: Reservation | null,
  serviceTimeOfDay: ServiceTimesTimeRange | null
): TimeRange | undefined {
  return reservation && reservation.type === 'TIMES'
    ? {
        startTime: reservation.startTime,
        endTime: reservation.endTime
      }
    : serviceTimeOfDay
    ? { startTime: serviceTimeOfDay.start, endTime: serviceTimeOfDay.end }
    : undefined
}

interface Props {
  day: OperationalDay
  dataForAllDays: Record<JsonOf<LocalDate>, ChildRecordOfDay>
  rowIndex: number
  editState?: EditState
  deleteAbsence: (i: number, d: LocalDate) => void
  updateReservation: (i: number, d: LocalDate, r: JsonOf<TimeRange>) => void
  saveReservation: (d: LocalDate) => void
  updateAttendance: (i: number, d: LocalDate, r: JsonOf<TimeRange>) => void
  saveAttendance: (d: LocalDate) => void
}

export default React.memo(function ChildDay({
  day,
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

  const dailyData = useMemo(
    () => dataForAllDays[day.date.formatIso()],
    [dataForAllDays, day.date]
  )

  const attendanceStartsOnPrevDay = useMemo(() => {
    const prevDay = dataForAllDays[day.date.subDays(1).formatIso()]
    return (
      dailyData?.attendance?.startTime === '00:00' &&
      (prevDay === undefined || prevDay?.attendance?.endTime === '23:59')
    )
  }, [dailyData, dataForAllDays, day.date])

  const attendanceEndsOnNextDay = useMemo(() => {
    const nextDay = dataForAllDays[day.date.addDays(1).formatIso()]
    return (
      dailyData?.attendance?.endTime === '23:59' &&
      (nextDay === undefined || nextDay.attendance?.startTime === '00:00')
    )
  }, [dailyData, dataForAllDays, day.date])

  const renderTime = useCallback(
    (time: string | null | undefined, sameDay: boolean) => {
      if (!sameDay) return '→'
      if (time && time !== '') return time
      return '–'
    },
    []
  )

  if (!dailyData) return null

  if (day.isHoliday && !dailyData.reservation && !dailyData.attendance)
    return null

  const { reservation, dailyServiceTimes, inOtherUnit, isInBackupGroup } =
    dailyData

  const serviceTimeOfDay =
    dailyServiceTimes === null || isVariableTime(dailyServiceTimes)
      ? null
      : isRegular(dailyServiceTimes)
      ? dailyServiceTimes.regularTimes
      : isIrregular(dailyServiceTimes)
      ? getTimesOnWeekday(dailyServiceTimes, day.date.getIsoDayOfWeek())
      : null

  const absence = editState
    ? editState.absences[rowIndex][day.date.formatIso()]
    : dailyData.absence

  const expectedAttendanceTime = getExpectedAttendanceTime(
    reservation,
    serviceTimeOfDay
  )

  return (
    <DateCell>
      <TimesRow data-qa={`reservation-${day.date.formatIso()}-${rowIndex}`}>
        {inOtherUnit ? (
          <TimeCell data-qa="in-other-unit">
            <Light>{i18n.unit.attendanceReservations.inOtherUnit}</Light>
          </TimeCell>
        ) : isInBackupGroup ? (
          <TimeCell data-qa="in-other-group">
            <Light>{i18n.unit.attendanceReservations.inOtherGroup}</Light>
          </TimeCell>
        ) : absence ? (
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
        ) : reservation && reservation.type === 'TIMES' ? (
          // a reservation exists for this day
          <>
            <ReservationTime data-qa="reservation-start">
              {reservation.startTime.format()}
            </ReservationTime>
            <ReservationTime data-qa="reservation-end">
              {reservation.endTime.format()}
            </ReservationTime>
          </>
        ) : day.isInOpenHolidayPeriod && reservation == null ? (
          // holiday period with upcoming deadline, no reservation yet
          <ReservationTime warning data-qa="holiday-reservation-missing">
            {i18n.unit.attendanceReservations.missingHolidayReservation}
          </ReservationTime>
        ) : serviceTimeOfDay ? (
          // daily service times
          <>
            <ReservationTime data-qa="reservation-start">
              {serviceTimeOfDay.start.format()}{' '}
              {i18n.unit.attendanceReservations.serviceTimeIndicator}
            </ReservationTime>
            <ReservationTime data-qa="reservation-end">
              {serviceTimeOfDay.end.format()}{' '}
              {i18n.unit.attendanceReservations.serviceTimeIndicator}
            </ReservationTime>
          </>
        ) : (
          // otherwise show missing service time indicator
          <ReservationTime warning data-qa="reservation-missing">
            {i18n.unit.attendanceReservations.missingServiceTime}
          </ReservationTime>
        )}
      </TimesRow>
      {!inOtherUnit && !isInBackupGroup ? (
        <TimesRow data-qa={`attendance-${day.date.formatIso()}-${rowIndex}`}>
          {editState &&
          day.date.isEqualOrBefore(LocalDate.todayInSystemTz()) ? (
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
                  expectedAttendanceTime?.startTime,
                  LocalTime.tryParse(dailyData.attendance?.startTime ?? '')
                )}
              >
                {renderTime(
                  dailyData.attendance?.startTime,
                  !attendanceStartsOnPrevDay
                )}
              </AttendanceTime>
              <AttendanceTime
                data-qa="attendance-end"
                warning={attendanceTimeDiffers(
                  LocalTime.tryParse(dailyData.attendance?.endTime ?? ''),
                  expectedAttendanceTime?.endTime
                )}
              >
                {renderTime(
                  dailyData.attendance?.endTime,
                  !attendanceEndsOnNextDay
                )}
              </AttendanceTime>
            </>
          )}
        </TimesRow>
      ) : null}
    </DateCell>
  )
})

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
