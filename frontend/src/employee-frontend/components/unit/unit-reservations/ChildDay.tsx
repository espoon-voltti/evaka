// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { css } from 'styled-components'
import {
  getTimesOnWeekday,
  isIrregular,
  isRegular,
  isVariableTime,
  TimeRange
} from 'lib-common/api-types/child/common'
import {
  ChildReservations,
  OperationalDay
} from 'lib-common/api-types/reservations'
import { Reservation } from 'lib-common/generated/api-types/reservations'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'
import { useTranslation } from '../../../state/i18n'
import AbsenceDay from './AbsenceDay'

function timeToMinutes(expected: string): number {
  const [hours, minutes] = expected.split(':').map(Number)
  return hours * 60 + minutes
}

/**
 * "Best effort" time difference calculation based on two "HH:mm" local time
 * strings without date part. This is only used for highlighting in the calendar
 * so edge cases are expected and might not matter much.
 *
 * The functionality can be improved once the reservation data model supports
 * reservations that span multiple days.
 */
function attendanceTimeDiffers(
  first: string | null | undefined,
  second: string | null | undefined,
  thresholdMinutes = 15
): boolean {
  if (!(first && second)) {
    return false
  }
  return timeToMinutes(first) - timeToMinutes(second) > thresholdMinutes
}

type ReservationOrServiceTime =
  | (TimeRange & { type: 'reservation' | 'service-time' })
  | undefined
const getReservationOrServiceTimeOfDay = (
  reservations: Reservation[],
  serviceTimeOfDay: TimeRange | null
): ReservationOrServiceTime =>
  reservations.length > 0
    ? {
        start: reservations[0].startTime,
        end: reservations[0].endTime,
        type: 'reservation'
      }
    : serviceTimeOfDay
    ? { ...serviceTimeOfDay, type: 'service-time' }
    : undefined

interface Props {
  day: OperationalDay
  childReservations: ChildReservations
}

export default React.memo(function ChildDay({ day, childReservations }: Props) {
  const { i18n } = useTranslation()

  const dailyData = childReservations.dailyData[day.date.formatIso()]

  if (!dailyData) return null

  if (
    day.isHoliday &&
    dailyData.reservations.length === 0 &&
    !dailyData.attendance
  )
    return null

  const serviceTimes = childReservations.child.dailyServiceTimes
  const serviceTimesAvailable =
    serviceTimes !== null && !isVariableTime(serviceTimes)
  const serviceTimeOfDay =
    serviceTimes === null || isVariableTime(serviceTimes)
      ? null
      : isRegular(serviceTimes)
      ? serviceTimes.regularTimes
      : isIrregular(serviceTimes)
      ? getTimesOnWeekday(serviceTimes, day.date.getIsoDayOfWeek())
      : null

  const expectedTimeForThisDay = getReservationOrServiceTimeOfDay(
    dailyData.reservations,
    serviceTimeOfDay
  )
  const maybeAsterisk = expectedTimeForThisDay?.type === 'service-time' && '*'

  return (
    <DateCell>
      <TimesRow>
        {dailyData.absence ? (
          <AbsenceDay type={dailyData.absence.type} />
        ) : expectedTimeForThisDay ? (
          /* show actual reservation or service time if exists */
          <>
            <ReservationTime>
              {expectedTimeForThisDay.start}
              {maybeAsterisk}
            </ReservationTime>
            <ReservationTime>
              {expectedTimeForThisDay.end}
              {maybeAsterisk}
            </ReservationTime>
          </>
        ) : serviceTimesAvailable && serviceTimeOfDay === null ? (
          /* else if daily service times are known but there is none for this day of week, show day off */
          <ReservationTime>
            {i18n.unit.attendanceReservations.dayOff}
          </ReservationTime>
        ) : (
          /* else show missing service time */
          <ReservationTime warning>
            {i18n.unit.attendanceReservations.missingServiceTime}
          </ReservationTime>
        )}
      </TimesRow>
      {dailyData.reservations.length > 1 && (
        <TimesRow>
          <ReservationTime>
            {dailyData.reservations[1].startTime ?? '–'}
          </ReservationTime>
          <ReservationTime>
            {dailyData.reservations[1].endTime ?? '–'}
          </ReservationTime>
        </TimesRow>
      )}
      <TimesRow>
        <AttendanceTime
          warning={attendanceTimeDiffers(
            expectedTimeForThisDay?.start,
            dailyData.attendance?.startTime
          )}
        >
          {dailyData.attendance?.startTime ?? '–'}
        </AttendanceTime>
        <AttendanceTime
          warning={attendanceTimeDiffers(
            dailyData.attendance?.endTime,
            expectedTimeForThisDay?.end
          )}
        >
          {dailyData.attendance?.endTime ?? '–'}
        </AttendanceTime>
      </TimesRow>
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
