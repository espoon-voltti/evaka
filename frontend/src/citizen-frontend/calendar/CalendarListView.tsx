// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import _ from 'lodash'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus } from 'lib-icons'
import LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import WeekElem, { WeekProps } from './WeekElem'
import { defaultMargins } from 'lib-components/white-space'
import Button from 'lib-components/atoms/buttons/Button'
import { useTranslation } from '../localization'

export interface Props {
  weeklyData: WeekProps[]
  onHoverButtonClick: () => void
  selectDate: (date: LocalDate) => void
}

export default React.memo(function CalendarListView({
  weeklyData,
  onHoverButtonClick,
  selectDate
}: Props) {
  const i18n = useTranslation()

  return (
    <>
      <FixedSpaceColumn spacing={'zero'}>
        {weeklyData.map((w) => (
          <WeekElem {...w} key={w.weekNumber} selectDate={selectDate} />
        ))}
      </FixedSpaceColumn>
      <HoverButton onClick={onHoverButtonClick} primary type="button">
        <Icon icon={faPlus} />
        {i18n.calendar.newReservationOrAbsence}
      </HoverButton>
    </>
  )
})

const HoverButton = styled(Button)`
  position: fixed;
  bottom: ${defaultMargins.s};
  right: ${defaultMargins.s};
  border-radius: 40px;
`

const Icon = styled(FontAwesomeIcon)`
  margin-right: ${defaultMargins.xs};
`
