// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import partition from 'lodash/partition'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import type {
  ChildReservationInfo,
  ReservationChildInfo
} from 'lib-common/generated/api-types/reservations'
import type LocalDate from 'lib-common/local-date'
import type LocalTime from 'lib-common/local-time'
import { useQueryResult } from 'lib-common/query'
import { reservationHasTimes } from 'lib-common/reservations'
import { theme } from 'lib-customizations/common'

import { renderResult } from '../async-rendering'
import { getServiceTimeRangeOrNullForDate } from '../common/dailyServiceTimes'
import type { UnitOrGroup } from '../common/unit-or-group'

import ChildSubListItem from './ChildSubListItem'
import { confirmedDayReservationsQuery } from './queries'

interface ChildReservationListProps {
  unitOrGroup: UnitOrGroup
  date: LocalDate
}

const ChildListContainer = styled.div``
type CategoryInfo = {
  sortCategory: number
  sortStartTime?: LocalTime
  sortEndTime?: LocalTime
}

export type CategorizedReservationInfo = ReservationChildInfo &
  CategoryInfo &
  ChildReservationInfo

const ChildSubList = styled.div`
  & > .present ~ .absent:not(.absent ~ .absent) {
    border-top: 1px dashed ${theme.colors.grayscale.g35};
  }

  margin-top: 5px;
`

export default React.memo(function ChildReservationList({
  unitOrGroup,
  date
}: ChildReservationListProps) {
  const confirmedDayReservationsResult = useQueryResult(
    confirmedDayReservationsQuery({
      unitId: unitOrGroup.unitId,
      examinationDate: date
    })
  )

  const sortStartTimeNullsLast = (o: CategorizedReservationInfo) =>
    o.sortStartTime || undefined
  const sortEndTimeNullsLast = (o: CategorizedReservationInfo) =>
    o.sortEndTime || undefined

  const filteredChildReservations: Result<CategorizedReservationInfo[]> =
    useMemo(
      () =>
        confirmedDayReservationsResult.map((result) => {
          const groupReservations =
            unitOrGroup.type === 'unit'
              ? result.childReservations
              : result.childReservations.filter(
                  (res) => res.groupId === unitOrGroup.id
                )

          const childMap = result.children

          return orderBy(
            groupReservations.map((ri) => {
              const childInfo = childMap[ri.childId]!
              let categoryInfo: CategoryInfo

              const [withTimes] = partition(
                ri.reservations,
                reservationHasTimes
              )
              if (withTimes.length > 0) {
                categoryInfo = {
                  sortCategory: 1,
                  sortStartTime: withTimes[0].range.start.asLocalTime(),
                  sortEndTime:
                    withTimes[withTimes.length - 1].range.end.asLocalTime()
                }
              } else if (ri.dailyServiceTimes != null) {
                const times = getServiceTimeRangeOrNullForDate(
                  ri.dailyServiceTimes,
                  date
                )
                if (times != null) {
                  categoryInfo = {
                    sortCategory: 1,
                    sortStartTime: times.start.asLocalTime(),
                    sortEndTime: times.end.asLocalTime()
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
                  sortStartTime: withTimes[0].range.start.asLocalTime(),
                  sortEndTime:
                    withTimes[withTimes.length - 1].range.end.asLocalTime()
                }
              if (ri.absent) categoryInfo = { sortCategory: 4 }
              if (ri.backupPlacement === 'OUT_ON_BACKUP_PLACEMENT')
                categoryInfo = { sortCategory: 5 }
              if (ri.scheduleType === 'FIXED_SCHEDULE')
                categoryInfo = { sortCategory: 2 }
              if (ri.scheduleType === 'TERM_BREAK')
                categoryInfo = { sortCategory: 6 }
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
          )
        }),
      [confirmedDayReservationsResult, unitOrGroup, date]
    )

  return (
    <ChildListContainer>
      {renderResult(filteredChildReservations, (reservations) =>
        reservations.length > 0 ? (
          <ChildSubList>
            {reservations.map((res) => (
              <ChildSubListItem
                date={date}
                reservationData={res}
                key={`${res.id}-${date.formatIso()}`}
              />
            ))}
          </ChildSubList>
        ) : null
      )}
    </ChildListContainer>
  )
})
