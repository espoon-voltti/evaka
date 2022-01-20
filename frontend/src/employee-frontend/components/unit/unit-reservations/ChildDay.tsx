// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { css } from 'styled-components'
import {
  getTimesOnWeekday,
  isIrregular,
  isRegular,
  isVariableTime
} from 'lib-common/api-types/child/common'
import {
  ChildReservations,
  OperationalDay
} from 'lib-common/api-types/reservations'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { colors } from 'lib-customizations/common'
import { useTranslation } from '../../../state/i18n'
import AbsenceDay from './AbsenceDay'

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

  return (
    <DateCell>
      <TimesRow>
        {dailyData.absence ? (
          <AbsenceDay type={dailyData.absence.type} />
        ) : dailyData.reservations.length > 0 ? (
          /* show actual reservation if it exists */
          <>
            <ReservationTime>
              {dailyData.reservations[0].startTime}
            </ReservationTime>
            <ReservationTime>
              {dailyData.reservations[0].endTime}
            </ReservationTime>
          </>
        ) : serviceTimesAvailable && serviceTimeOfDay ? (
          /* else show service time if it is known for that day of week */
          <>
            <ReservationTime>{serviceTimeOfDay.start}*</ReservationTime>
            <ReservationTime>{serviceTimeOfDay.end}*</ReservationTime>
          </>
        ) : serviceTimesAvailable && serviceTimeOfDay === null ? (
          /* else if daily service times are known but there is none for this day of week, show day off */
          <ReservationTime>
            {i18n.unit.attendanceReservations.dayOff}
          </ReservationTime>
        ) : (
          /* else show no reservation */
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
        <AttendanceTime>
          {dailyData.attendance?.startTime ?? '–'}
        </AttendanceTime>
        <AttendanceTime>{dailyData.attendance?.endTime ?? '–'}</AttendanceTime>
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

const TimeCell = styled.div`
  flex: 1 0 54px;
  text-align: center;
`

const AttendanceTime = styled(TimeCell)`
  font-weight: ${fontWeights.semibold};
`

const ReservationTime = styled(TimeCell)<{ warning?: boolean }>`
  ${(p) =>
    p.warning &&
    css`
      color: ${colors.accents.a2orangeDark};
    `};
`
