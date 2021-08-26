// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import _ from 'lodash'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { DailyReservationData } from './api'
import WeekElem, { WeekProps } from './WeekElem'
import styled from 'styled-components'
import { defaultMargins } from 'lib-components/white-space'
import Button from 'lib-components/atoms/buttons/Button'

export interface Props {
  dailyReservations: DailyReservationData[]
  onCreateReservationClicked: () => void
}

export default React.memo(function CalendarListView({
  dailyReservations,
  onCreateReservationClicked
}: Props) {
  const weeklyData = dailyReservations.reduce((weekly, daily) => {
    const last = _.last(weekly)
    if (last === undefined || daily.date.getIsoWeek() !== last.weekNumber) {
      return [
        ...weekly,
        { weekNumber: daily.date.getIsoWeek(), dailyReservations: [daily] }
      ]
    } else {
      return [
        ..._.dropRight(weekly),
        {
          ...last,
          dailyReservations: [...last.dailyReservations, daily]
        }
      ]
    }
  }, [] as WeekProps[])

  return (
    <div>
      <FixedSpaceColumn spacing={'zero'}>
        {weeklyData.map((w) => (
          <WeekElem {...w} key={w.weekNumber} />
        ))}
      </FixedSpaceColumn>
      <HoverButton
        onClick={onCreateReservationClicked}
        text={'Luo varaus'}
        primary
        type="button"
      />
    </div>
  )
})

const HoverButton = styled(Button)`
  position: fixed;
  bottom: ${defaultMargins.s};
  right: ${defaultMargins.s};
  border-radius: 40px;
`
