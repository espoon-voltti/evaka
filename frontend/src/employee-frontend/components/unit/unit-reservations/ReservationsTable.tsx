// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import {
  Child,
  ChildReservations,
  OperationalDay
} from 'lib-common/api-types/reservations'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H4 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faCalendarPlus } from 'lib-icons'
import { useTranslation } from '../../../state/i18n'
import AgeIndicatorIcon from '../../common/AgeIndicatorIcon'
import ChildDay, { TimeCell, TimesRow } from './ChildDay'

interface Props {
  operationalDays: OperationalDay[]
  reservations: ChildReservations[]
  onMakeReservationForChild: (child: Child) => void
}

export default React.memo(function ReservationsTable({
  operationalDays,
  reservations,
  onMakeReservationForChild
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
                  i18n.datePicker.weekdaysShort[date.getIsoDayOfWeek() - 1]
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
          <CustomTh />
        </Tr>
      </Thead>
      <Tbody>
        {reservations.map((childReservations) => {
          const childName = `${childReservations.child.firstName} ${childReservations.child.lastName}`

          return (
            <Tr key={childName}>
              <NameTd>
                <ChildName>
                  <AgeIndicatorIcon
                    dateOfBirth={childReservations.child.dateOfBirth}
                  />
                  <Link to={`/child-information/${childReservations.child.id}`}>
                    {childName}
                  </Link>
                </ChildName>
              </NameTd>
              {operationalDays.map((day) => (
                <StyledTd key={day.date.formatIso()}>
                  <ChildDay day={day} childReservations={childReservations} />
                </StyledTd>
              ))}
              <StyledTd>
                <IconButton
                  icon={faCalendarPlus}
                  onClick={() =>
                    onMakeReservationForChild(childReservations.child)
                  }
                />
              </StyledTd>
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

const NameTd = styled(StyledTd)`
  width: 350px;
`

const ChildName = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: flex-start;
  align-items: center;

  a {
    margin-left: ${defaultMargins.xs};
  }
`

const Date = styled(H4)`
  text-align: center;
  margin: 0 0 ${defaultMargins.s};
`
