// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import _ from 'lodash'
import LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import WeekElem, { WeekProps } from './WeekElem'
import styled from 'styled-components'
import { defaultMargins } from 'lib-components/white-space'
import Button from 'lib-components/atoms/buttons/Button'

export interface Props {
  weeklyData: WeekProps[]
  onCreateReservationClicked: () => void
  selectDate: (date: LocalDate) => void
}

export default React.memo(function CalendarListView({
  weeklyData,
  onCreateReservationClicked,
  selectDate
}: Props) {
  return (
    <>
      <FixedSpaceColumn spacing={'zero'}>
        {weeklyData.map((w) => (
          <WeekElem {...w} key={w.weekNumber} selectDate={selectDate} />
        ))}
      </FixedSpaceColumn>
      <HoverButton
        onClick={onCreateReservationClicked}
        text={'Luo varaus'}
        primary
        type="button"
      />
    </>
  )
})

const HoverButton = styled(Button)`
  position: fixed;
  bottom: ${defaultMargins.s};
  right: ${defaultMargins.s};
  border-radius: 40px;
`
