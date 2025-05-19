// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import type { DayReservationStatisticsResult } from 'lib-common/generated/api-types/reservations'
import colors from 'lib-customizations/common'

import { useTranslation } from '../common/i18n'
import type { UnitOrGroup } from '../common/unit-or-group'

import DayListItem, { ChevronBox, DateBox } from './DayListItem'

interface Props {
  unitOrGroup: UnitOrGroup
  reservationStatistics: DayReservationStatisticsResult[]
}

export default React.memo(function DayList({
  unitOrGroup,
  reservationStatistics
}: Props) {
  const { i18n } = useTranslation()

  const sortedDays = useMemo(
    () => orderBy(reservationStatistics, ['date']),
    [reservationStatistics]
  )
  return (
    <>
      <HeaderBox>
        <DateBox />
        <DayListTitle>{i18n.attendances.status.COMING}</DayListTitle>
        <DayListTitle>{i18n.attendances.status.ABSENT}</DayListTitle>
        <ChevronBox />
      </HeaderBox>
      <NoMarginList>
        {sortedDays.map((dr) => (
          <Li
            key={`${dr.date.format()}-li`}
            data-qa={`day-item-${dr.date.formatIso()}`}
          >
            <DayListItem
              key={`${dr.date.format()}-dli`}
              dayStats={dr}
              unitOrGroup={unitOrGroup}
            />
          </Li>
        ))}
      </NoMarginList>
    </>
  )
})

const HeaderBox = styled.div`
  margin-left: 24px;
  flex-grow: 1;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  min-height: 40px;
`

const DayListTitle = styled.span`
  font-family: Montserrat, sans-serif;
  font-size: 14px;
  line-height: 16px;
  letter-spacing: 0.08em;
  font-weight: bold;
  text-transform: uppercase;
  text-decoration: none;
`

const NoMarginList = styled.ol`
  list-style: none;
  padding: 0;
  margin-top: 0;

  li {
    margin-bottom: 0;

    &:last-child {
      margin-bottom: 0;
    }
  }
`

const Li = styled.li`
  &:after {
    content: '';
    width: 100%;
    background: ${colors.grayscale.g15};
    height: 2px;
    display: block;
    position: absolute;
  }
`
