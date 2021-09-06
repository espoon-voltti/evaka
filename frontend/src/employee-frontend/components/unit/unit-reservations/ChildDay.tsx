import React from 'react'
import styled from 'styled-components'
import { ChildReservations, OperationalDay } from '../../../api/unit'
import {
  getTimesOnWeekday,
  isIrregular,
  isRegular,
  isVariableTime
} from 'lib-common/api-types/child/common'
import { defaultMargins, Gap } from 'lib-components/white-space'
import AbsenceDay from './AbsenceDay'

interface Props {
  day: OperationalDay
  childReservations: ChildReservations
}

export default React.memo(function ChildDay({ day, childReservations }: Props) {
  const dailyData = childReservations.dailyData[day.date.formatIso()]

  if (!dailyData) return null

  if (day.isHoliday && !dailyData.reservation && !dailyData.attendance)
    return null

  if (dailyData.absence && !dailyData.attendance)
    return <AbsenceDay type={dailyData.absence.type} />

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
      <AttendanceTimesRow>
        <TimeCell>{dailyData.attendance?.startTime ?? '–'}</TimeCell>
        <TimeCell>{dailyData.attendance?.endTime ?? '–'}</TimeCell>
      </AttendanceTimesRow>
      <Gap size="xxs" />
      <ReservationTimesRow>
        {dailyData.reservation ? (
          /* show actual reservation if it exists */
          <>
            <ReservationTime>
              {dailyData.reservation?.startTime ?? '–'}
            </ReservationTime>
            <ReservationTime>
              {dailyData.reservation?.endTime ?? '–'}
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
          <ReservationTime>Vapaapäivä</ReservationTime>
        ) : (
          /* else show no reservation */
          <ReservationTime>Ei varausta</ReservationTime>
        )}
      </ReservationTimesRow>
    </DateCell>
  )
})

const DateCell = styled.div`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
`

export const TimesRow = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-evenly;
`

const AttendanceTimesRow = styled(TimesRow)`
  font-weight: 600;
`

const ReservationTimesRow = styled(TimesRow)``

export const TimeCell = styled.div`
  min-width: 54px;
  text-align: center;

  &:not(:first-child) {
    margin-left: ${defaultMargins.xs};
  }
`

const ReservationTime = styled(TimeCell)`
  font-style: italic;
`
