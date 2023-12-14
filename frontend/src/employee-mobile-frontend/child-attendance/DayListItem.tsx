// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faChevronDown, faChevronUp } from 'Icons'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import { useSelectedGroup } from 'employee-mobile-frontend/common/selected-group'
import { DayReservationStatisticsResult } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import colors, { theme } from 'lib-customizations/common'

import { useTranslation } from '../common/i18n'

import ChildReservationList from './ChildReservationList'

interface DayListItemProps {
  dayStats: DayReservationStatisticsResult
}

export default React.memo(function DayListItem({ dayStats }: DayListItemProps) {
  const { i18n, lang } = useTranslation()

  const [isOpen, setOpen] = useState<boolean>(false)

  const { selectedGroupId } = useSelectedGroup()

  const tomorrow = LocalDate.todayInHelsinkiTz().addDays(1)

  const filteredStats = useMemo(() => {
    const relevantGroupStats =
      selectedGroupId.type === 'all'
        ? dayStats.groupStatistics
        : dayStats.groupStatistics.map((day) => ({
            ...day,
            reservationInfos: dayStats.groupStatistics.filter(
              (i) => i.groupId === selectedGroupId.id
            )
          }))

    return relevantGroupStats.reduce(
      (prev, next) => ({
        presentCount: prev.presentCount + next.presentCount,
        calculatedPresent: prev.calculatedPresent + next.calculatedPresent,
        absent: prev.absent + next.absentCount
      }),
      { presentCount: 0, calculatedPresent: 0, absent: 0 }
    )
  }, [dayStats, selectedGroupId])

  return (
    <>
      <DayBox>
        <DayBoxInfo>
          <DateBox>
            {dayStats.date.isEqual(tomorrow) && (
              <span>{i18n.attendances.confirmedDays.tomorrow}</span>
            )}
            <span data-qa="date">
              {dayStats.date.format('EEEEEE d.M.', lang)}
            </span>
          </DateBox>
          <CountBox spacing="xxs" alignItems="center">
            <span data-qa="present-total">{filteredStats.presentCount}</span>
            <span data-qa="present-calc">{`(${filteredStats.calculatedPresent.toLocaleString(
              lang
            )})`}</span>
          </CountBox>
          <CountBox spacing="xxs" alignItems="center">
            <span data-qa="absent-total">{filteredStats.absent}</span>
          </CountBox>
          <ChevronBox
            data-qa="open-day-button"
            onClick={() => setOpen(!isOpen)}
          >
            <span>
              <AccordionIcon
                icon={isOpen ? faChevronUp : faChevronDown}
                color={theme.colors.main.m2}
              />
            </span>
          </ChevronBox>
        </DayBoxInfo>
      </DayBox>
      {isOpen && filteredStats.absent + filteredStats.presentCount > 0 && (
        <ChildReservationList date={dayStats.date} />
      )}
    </>
  )
})

export const ChevronBox = styled.div`
  min-width: 32px;
  padding: 0;
`

export const DateBox = styled.div`
  display: flex;
  flex-direction: column;
  min-width: 84px;
`

const CountBox = styled(FixedSpaceColumn)`
  min-width: 60px;
`

const AccordionIcon = styled(FontAwesomeIcon)`
  cursor: pointer;
  color: ${theme.colors.main.m1};
  padding-right: 1em;
`

const DayBoxInfo = styled.div`
  flex-grow: 1;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  justify-content: space-between;
  min-height: 56px;
`
const DayBox = styled.div`
  align-items: center;
  display: flex;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  border-radius: 2px;
  background-color: ${colors.grayscale.g0};
  box-shadow: 0 3px 3px rgba(0, 0, 0, 0.15);
`
