// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled, { css } from 'styled-components'
import {
  Child,
  ChildReservations,
  OperationalDay
} from 'lib-common/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { fontWeights, H4 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faCalendarPlus } from 'lib-icons'
import { useTranslation } from '../../../state/i18n'
import AgeIndicatorIcon from '../../common/AgeIndicatorIcon'
import ChildDay from './ChildDay'

interface Props {
  operationalDays: OperationalDay[]
  reservations: ChildReservations[]
  onMakeReservationForChild: (child: Child) => void
  selectedDate: LocalDate
}

export default React.memo(function ReservationsTable({
  operationalDays,
  reservations,
  onMakeReservationForChild,
  selectedDate
}: Props) {
  const { i18n, lang } = useTranslation()

  return (
    <Table>
      <Thead>
        <Tr>
          <CustomTh>{i18n.unit.attendanceReservations.childName}</CustomTh>
          {operationalDays.map(({ date, isHoliday }) => (
            <DateTh
              shrink
              key={date.formatIso()}
              faded={isHoliday}
              highlight={date.isToday()}
            >
              <Date centered highlight={date.isToday()}>
                {date.format('EEEEEE dd.MM.', lang)}
              </Date>
              <DayHeader>
                <span>{i18n.unit.attendanceReservations.startTime}</span>
                <span>{i18n.unit.attendanceReservations.endTime}</span>
              </DayHeader>
            </DateTh>
          ))}
          <CustomTh shrink />
        </Tr>
      </Thead>
      <Tbody>
        {reservations.map((childReservations) => {
          const childName = `${childReservations.child.firstName} ${childReservations.child.lastName}`

          return (
            <Tr key={childReservations.child.id}>
              <NameTd>
                <ChildName>
                  <AgeIndicatorIcon
                    isUnder3={
                      selectedDate.differenceInYears(
                        childReservations.child.dateOfBirth
                      ) < 3
                    }
                  />
                  <Link to={`/child-information/${childReservations.child.id}`}>
                    {childName}
                  </Link>
                </ChildName>
              </NameTd>
              {operationalDays.map((day) => (
                <DayTd
                  key={day.date.formatIso()}
                  highlight={day.date.isToday()}
                >
                  <ChildDay day={day} childReservations={childReservations} />
                </DayTd>
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

const CustomTh = styled(Th)<{ shrink?: boolean }>`
  vertical-align: bottom;
  ${(p) =>
    p.shrink &&
    css`
      width: 0; // causes the column to take as little space as possible
    `}
`

const DateTh = styled(CustomTh)<{ faded: boolean; highlight: boolean }>`
  ${(p) => p.faded && `color: ${colors.grayscale.g35};`}
  ${(p) => p.highlight && `border-bottom: 2px solid ${colors.main.m3};`}
`

const DayHeader = styled.div`
  display: flex;
  justify-content: space-evenly;
  gap: ${defaultMargins.xs};
`

const StyledTd = styled(Td)`
  border-right: 1px solid ${colors.grayscale.g35};
  vertical-align: middle;
`
const DayTd = styled(StyledTd)<{ highlight: boolean }>`
  ${(p) =>
    p.highlight &&
    css`
      border-left: 2px solid ${colors.main.m3};
      border-right: 2px solid ${colors.main.m3};
    `}
  padding: 0;
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

const Date = styled(H4)<{ highlight: boolean }>`
  margin: 0 0 ${defaultMargins.s};
  ${(p) =>
    p.highlight &&
    css`
      color: ${colors.main.m2};
      font-weight: ${fontWeights.semibold};
    `}
`
