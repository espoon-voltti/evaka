// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import _ from 'lodash'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus } from 'lib-icons'
import LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import WeekElem from './WeekElem'
import { defaultMargins } from 'lib-components/white-space'
import Button from 'lib-components/atoms/buttons/Button'
import { useTranslation } from '../localization'
import { DailyReservationData } from 'lib-common/generated/api-types/reservations'

export interface Props {
  dailyData: DailyReservationData[]
  onHoverButtonClick: () => void
  selectDate: (date: LocalDate) => void
  dayIsReservable: (dailyData: DailyReservationData) => boolean
}

export default React.memo(function CalendarListView({
  dailyData,
  onHoverButtonClick,
  selectDate,
  dayIsReservable
}: Props) {
  const i18n = useTranslation()
  const weeklyData = useMemo(() => asWeeklyData(dailyData), [dailyData])

  return (
    <>
      <FixedSpaceColumn spacing={'zero'}>
        {weeklyData.map((w) => (
          <WeekElem
            {...w}
            key={`${w.dailyReservations[0].date.formatIso()}`}
            selectDate={selectDate}
            dayIsReservable={dayIsReservable}
          />
        ))}
      </FixedSpaceColumn>
      <HoverButton
        onClick={onHoverButtonClick}
        primary
        type="button"
        data-qa="open-calendar-actions-modal"
      >
        <Icon icon={faPlus} />
        {i18n.calendar.newReservationOrAbsence}
      </HoverButton>
    </>
  )
})

export interface WeeklyData {
  weekNumber: number
  dailyReservations: DailyReservationData[]
}

export const asWeeklyData = (dailyData: DailyReservationData[]): WeeklyData[] =>
  dailyData.reduce<WeeklyData[]>((weekly, daily) => {
    const last = _.last(weekly)
    if (last === undefined || daily.date.getIsoWeek() !== last.weekNumber) {
      return [
        ...weekly,
        {
          weekNumber: daily.date.getIsoWeek(),
          dailyReservations: [daily]
        }
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
  }, [])

const HoverButton = styled(Button)`
  position: fixed;
  bottom: ${defaultMargins.s};
  right: ${defaultMargins.s};
  border-radius: 40px;
`

const Icon = styled(FontAwesomeIcon)`
  margin-right: ${defaultMargins.xs};
`
