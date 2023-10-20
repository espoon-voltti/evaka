// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faChevronDown, faChevronUp } from 'Icons'
import orderBy from 'lodash/orderBy'
import partition from 'lodash/partition'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import { getServiceTimeRangeOrNullForDate } from 'employee-mobile-frontend/common/dailyServiceTimes'
import {
  ChildDailyReservationInfo,
  ReservationChildInfo,
  UnitDailyReservationInfo
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { reservationHasTimes } from 'lib-common/reservations'
import { UUID } from 'lib-common/types'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import colors, { theme } from 'lib-customizations/common'

import { useTranslation } from '../common/i18n'

import ChildSubListItem from './ChildSubListItem'

const DayBox = styled.div`
  align-items: center;
  display: flex;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  border-radius: 2px;
  background-color: ${colors.grayscale.g0};
  box-shadow: 0 3px 3px rgba(0, 0, 0, 0.15);
`

type CategoryInfo = {
  sortCategory: number
  sortStartTime?: LocalTime
  sortEndTime?: LocalTime
}
export type CategorizedReservationInfo = ChildDailyReservationInfo &
  ReservationChildInfo &
  CategoryInfo

interface DayListItemProps {
  dailyReservationData: UnitDailyReservationInfo
  childMap: Record<UUID, ReservationChildInfo>
}

export default React.memo(function DayListItem({
  dailyReservationData,
  childMap
}: DayListItemProps) {
  const { i18n, lang } = useTranslation()

  const [isOpen, setOpen] = useState<boolean>(false)

  const getCalculatedNumber = (
    total: number,
    data: ChildDailyReservationInfo
  ) => total + data.occupancyCoefficient

  const tomorrow = LocalDate.todayInHelsinkiTz().addDays(1)
  const { presentTotal, absentTotal, presentCalc } = useMemo(() => {
    const presentChildren = dailyReservationData.reservationInfos.filter(
      (i) => !i.absent && !i.outOnBackupPlacement
    )
    const absentChildren = dailyReservationData.reservationInfos.filter(
      (i) => i.absent || i.outOnBackupPlacement
    )
    return {
      presentTotal: presentChildren.length,
      presentCalc: presentChildren.reduce(getCalculatedNumber, 0),
      absentTotal: absentChildren.length
    }
  }, [dailyReservationData])

  const sortStartTimeNullsLast = (o: CategorizedReservationInfo) =>
    o.sortStartTime || undefined
  const sortEndTimeNullsLast = (o: CategorizedReservationInfo) =>
    o.sortEndTime || undefined
  const sortedReservations: CategorizedReservationInfo[] = useMemo(
    () =>
      orderBy(
        dailyReservationData.reservationInfos.map((ri) => {
          const childInfo = childMap[ri.childId]
          let categoryInfo: CategoryInfo

          const [withTimes] = partition(ri.reservations, reservationHasTimes)
          if (ri.dailyServiceTimes != null) {
            const times = getServiceTimeRangeOrNullForDate(
              ri.dailyServiceTimes,
              dailyReservationData.date
            )
            if (times != null) {
              categoryInfo = {
                sortCategory: 1,
                sortStartTime: times.start,
                sortEndTime: times.end
              }
            } else {
              categoryInfo = {
                sortCategory: 2
              }
            }
          } else categoryInfo = { sortCategory: 3 }

          if (withTimes.length > 0)
            categoryInfo = {
              sortCategory: 1,
              sortStartTime: withTimes[0].startTime,
              sortEndTime: withTimes[withTimes.length - 1].endTime
            }
          if (ri.absent) categoryInfo = { sortCategory: 4 }
          if (ri.outOnBackupPlacement) categoryInfo = { sortCategory: 5 }
          return { ...ri, ...childInfo, ...categoryInfo }
        }),
        [
          'sortCategory',
          sortStartTimeNullsLast,
          sortEndTimeNullsLast,
          'lastName',
          'firstName'
        ],
        []
      ),
    [dailyReservationData, childMap]
  )

  return (
    <>
      <DayBox>
        <DayBoxInfo>
          <DateBox>
            {dailyReservationData.date.isEqual(tomorrow) && (
              <span>{i18n.attendances.confirmedDays.tomorrow}</span>
            )}
            <span>{dailyReservationData.date.format('EEEEEE d.M.', lang)}</span>
          </DateBox>
          <CountBox spacing="xxs" alignItems="center">
            <span>{presentTotal}</span>
            <span>{`(${presentCalc})`}</span>
          </CountBox>
          <CountBox spacing="xxs" alignItems="center">
            <span>{absentTotal}</span>
          </CountBox>
          <ChevronBox onClick={() => setOpen(!isOpen)}>
            <span>
              <AccordionIcon
                icon={isOpen ? faChevronUp : faChevronDown}
                color={theme.colors.main.m2}
              />
            </span>
          </ChevronBox>
        </DayBoxInfo>
      </DayBox>
      <ChildSubList>
        {sortedReservations.map(
          (childReservation) =>
            isOpen && (
              <ChildSubListItem
                key={`${dailyReservationData.date.format()}-${
                  childReservation.childId
                }`}
                reservationData={childReservation}
              />
            )
        )}
      </ChildSubList>
    </>
  )
})

const ChildSubList = styled.div`
  & > :first-child {
    margin-top: 5px;
    #border-top: 2px solid ${theme.colors.grayscale.g35};
  }

  & > .absent:not(.absent ~ .absent) {
    border-top: 1px dashed ${theme.colors.grayscale.g35};
  }
`

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
