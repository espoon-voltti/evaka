// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import { H4 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import colors from 'lib-customizations/common'
import { ChildReservations, OperationalDay } from 'employee-frontend/api/unit'
import { useTranslation } from 'employee-frontend/state/i18n'
import AgeIndicatorIcon from 'employee-frontend/components/common/AgeIndicatorIcon'
import { AbsenceType } from 'lib-common/generated/enums'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { faThermometer } from 'lib-icons'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import {
  getTimesOnWeekday,
  isIrregular,
  isRegular,
  isVariableTime
} from 'lib-common/api-types/child/common'

interface Props {
  operationalDays: OperationalDay[]
  reservations: ChildReservations[]
}

function renderAbsence(type: AbsenceType) {
  if (type === 'SICKLEAVE')
    return (
      <AbsenceCell>
        <FixedSpaceRow spacing="xs" alignItems="center">
          <RoundIcon
            content={faThermometer}
            color={colors.accents.violet}
            size="m"
          />
          <div>Sairaus</div>
        </FixedSpaceRow>
      </AbsenceCell>
    )

  return (
    <AbsenceCell>
      <FixedSpaceRow spacing="xs" alignItems="center">
        <RoundIcon content="–" color={colors.primary} size="m" />
        <div>Muu syy</div>
      </FixedSpaceRow>
    </AbsenceCell>
  )
}

function renderChildDay(
  day: OperationalDay,
  childReservations: ChildReservations
) {
  const dailyData = childReservations.dailyData.find((d) =>
    d.date.isEqual(day.date)
  )

  if (!dailyData) return null

  if (day.isHoliday && !dailyData.reservation && !dailyData.attendance)
    return null

  if (dailyData.absence && !dailyData.attendance)
    return renderAbsence(dailyData.absence.type)

  const serviceTimes = childReservations.child.dailyServiceTimes
  const serviceTimesAvailable =
    serviceTimes != null && !isVariableTime(serviceTimes)
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
        <Time>{dailyData.attendance?.startTime ?? '–'}</Time>
        <Time>{dailyData.attendance?.endTime ?? '–'}</Time>
      </AttendanceTimesRow>
      <Gap size="xxs" />
      <ReservationTimesRow>
        {dailyData.reservation ? (
          <>
            <ReservationTime>
              {dailyData.reservation?.startTime ?? '–'}
            </ReservationTime>
            <ReservationTime>
              {dailyData.reservation?.endTime ?? '–'}
            </ReservationTime>
          </>
        ) : serviceTimesAvailable && serviceTimeOfDay ? (
          <>
            <ReservationTime>{serviceTimeOfDay.start}*</ReservationTime>
            <ReservationTime>{serviceTimeOfDay.end}*</ReservationTime>
          </>
        ) : serviceTimesAvailable && serviceTimeOfDay === null ? (
          <ReservationTime>Vapaapäivä</ReservationTime>
        ) : (
          <ReservationTime>Ei varausta</ReservationTime>
        )}
      </ReservationTimesRow>
    </DateCell>
  )
}

export default React.memo(function ReservationsTable({
  operationalDays,
  reservations
}: Props) {
  const { i18n } = useTranslation()

  return (
    <Table>
      <Thead>
        <Tr>
          <CustomTh>{i18n.unit.attendanceReservations.childName}</CustomTh>
          {operationalDays.map(({ date, isHoliday }) => (
            <DateTh key={date.formatIso()} faded={isHoliday}>
              <Date>
                {`${
                  i18n.datePicker.weekdaysShort[date.getIsoDayOfWeek()]
                } ${date.format('dd.MM.')}`}
              </Date>
              <TimesRow>
                <Time>{i18n.unit.attendanceReservations.startTime}</Time>
                <Time>{i18n.unit.attendanceReservations.endTime}</Time>
              </TimesRow>
            </DateTh>
          ))}
        </Tr>
      </Thead>
      <Tbody>
        {reservations.map((childReservations) => {
          const childName = `${childReservations.child.firstName} ${childReservations.child.lastName}`

          return (
            <Tr key={childName}>
              <StyledTd>
                <ChildName>
                  <Link to={`/child-information/${childReservations.child.id}`}>
                    {childName}
                  </Link>
                  <AgeIndicatorIcon
                    dateOfBirth={childReservations.child.dateOfBirth}
                  />
                </ChildName>
              </StyledTd>
              {operationalDays.map((day) => (
                <StyledTd key={day.date.formatIso()}>
                  {renderChildDay(day, childReservations)}
                </StyledTd>
              ))}
            </Tr>
          )
        })}
      </Tbody>
    </Table>
  )
})

const CustomTh = styled(Th)`
  border: none;
  vertical-align: bottom;
`

const DateTh = styled(CustomTh)<{ faded: boolean }>`
  width: 0; // causes the column to take as little space as possible
  ${({ faded }) => (faded ? `color: ${colors.greyscale.medium};` : '')}
`

const StyledTd = styled(Td)`
  border-right: 1px solid ${colors.greyscale.medium};
  vertical-align: middle;
`

const ChildName = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: flex-start;
  align-items: center;

  a {
    margin-right: ${defaultMargins.xs};
  }
`

const Date = styled(H4)`
  text-align: center;
  margin: 0;
  margin-bottom: ${defaultMargins.s};
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
`

const AttendanceTimesRow = styled(TimesRow)`
  font-weight: 600;
`

const ReservationTimesRow = styled(TimesRow)``

const Time = styled.div`
  min-width: 54px;
  text-align: center;

  &:not(:first-child) {
    margin-left: ${defaultMargins.xs};
  }
`

const ReservationTime = styled(Time)`
  font-style: italic;
`

const AbsenceCell = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  font-style: italic;
`
