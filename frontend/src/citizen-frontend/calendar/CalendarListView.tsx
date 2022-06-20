// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import dropRight from 'lodash/dropRight'
import last from 'lodash/last'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import {
  DailyReservationData,
  ReservationChild
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import { faPlus } from 'lib-icons'

import { useTranslation } from '../localization'

import WeekElem from './WeekElem'

export interface Props {
  childData: ReservationChild[]
  dailyData: DailyReservationData[]
  onHoverButtonClick: () => void
  selectDate: (date: LocalDate) => void
  dayIsReservable: (dailyData: DailyReservationData) => boolean
  dayIsHolidayPeriod: (date: LocalDate) => boolean
}

export default React.memo(function CalendarListView({
  childData,
  dailyData,
  dayIsHolidayPeriod,
  onHoverButtonClick,
  selectDate,
  dayIsReservable
}: Props) {
  const i18n = useTranslation()
  const weeklyData = useMemo(() => asWeeklyData(dailyData), [dailyData])

  return (
    <>
      <FixedSpaceColumn spacing="zero">
        {weeklyData.map((w) => (
          <WeekElem
            {...w}
            key={w.dailyReservations[0].date.formatIso()}
            childData={childData}
            selectDate={selectDate}
            dayIsReservable={dayIsReservable}
            dayIsHolidayPeriod={dayIsHolidayPeriod}
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
    const lastWeek = last(weekly)
    if (
      lastWeek === undefined ||
      daily.date.getIsoWeek() !== lastWeek.weekNumber
    ) {
      return [
        ...weekly,
        {
          weekNumber: daily.date.getIsoWeek(),
          dailyReservations: [daily]
        }
      ]
    } else {
      return [
        ...dropRight(weekly),
        {
          ...lastWeek,
          dailyReservations: [...lastWeek.dailyReservations, daily]
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
