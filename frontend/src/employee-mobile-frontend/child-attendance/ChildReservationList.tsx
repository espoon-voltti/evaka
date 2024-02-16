// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import partition from 'lodash/partition'
import React, { useMemo } from 'react'
import styled from 'styled-components'

import { renderResult } from 'employee-mobile-frontend/async-rendering'
import { getServiceTimeRangeOrNullForDate } from 'employee-mobile-frontend/common/dailyServiceTimes'
import { useSelectedGroup } from 'employee-mobile-frontend/common/selected-group'
import { Result } from 'lib-common/api'
import {
  ChildReservationInfo,
  ReservationChildInfo
} from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { useQueryResult } from 'lib-common/query'
import { reservationHasTimes } from 'lib-common/reservations'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { theme } from 'lib-customizations/common'

import ChildSubListItem from './ChildSubListItem'
import { confirmedDayReservationsQuery } from './queries'

interface ChildReservationListProps {
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
  date
}: ChildReservationListProps) {
  const { unitId } = useNonNullableParams<{
    unitId: UUID
  }>()

  const { selectedGroupId } = useSelectedGroup()

  const confirmedDayReservationsResult = useQueryResult(
    confirmedDayReservationsQuery(unitId, date)
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
            selectedGroupId.type === 'all'
              ? result.childReservations
              : result.childReservations.filter(
                  (res) => res.groupId === selectedGroupId.id
                )

          const childMap = result.children

          return orderBy(
            groupReservations.map((ri) => {
              const childInfo = childMap[ri.childId]
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
              if (ri.outOnBackupPlacement) categoryInfo = { sortCategory: 5 }
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
      [confirmedDayReservationsResult, selectedGroupId, date]
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
