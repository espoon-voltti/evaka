// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import { H4 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import colors from 'lib-customizations/common'
import { ChildReservations, OperationalDay } from 'employee-frontend/api/unit'
import { useTranslation } from 'employee-frontend/state/i18n'
import AgeIndicatorIcon from 'employee-frontend/components/common/AgeIndicatorIcon'
import ChildDay, { TimeCell, TimesRow } from './ChildDay'

interface Props {
  operationalDays: OperationalDay[]
  reservations: ChildReservations[]
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
                <TimeCell>
                  {i18n.unit.attendanceReservations.startTime}
                </TimeCell>
                <TimeCell>{i18n.unit.attendanceReservations.endTime}</TimeCell>
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
                  <ChildDay day={day} childReservations={childReservations} />
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
  margin: 0 0 ${defaultMargins.s};
`
