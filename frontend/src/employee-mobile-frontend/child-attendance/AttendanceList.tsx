// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useEffect, useMemo, useState } from 'react'

import type {
  AttendanceChild,
  AttendanceStatus
} from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalTime from 'lib-common/local-time'
import TimeRangeEndpoint from 'lib-common/time-range-endpoint'
import type { UUID } from 'lib-common/types'
import { ContentArea } from 'lib-components/layout/Container'
import { TabLinks } from 'lib-components/molecules/Tabs'

import { routes } from '../App'
import { getServiceTimeRangeOrNullForDate } from '../common/dailyServiceTimes'
import { useTranslation } from '../common/i18n'
import type { UnitOrGroup } from '../common/unit-or-group'

import type { ListItem, SortType } from './ChildList'
import ChildList from './ChildList'
import type { AttendanceStatuses } from './utils'
import { childAttendanceStatus } from './utils'

interface Props {
  unitOrGroup: UnitOrGroup
  activeStatus: AttendanceStatus
  unitChildren: AttendanceChild[]
  attendanceStatuses: AttendanceStatuses
}

export default React.memo(function AttendanceList({
  unitOrGroup,
  activeStatus,
  unitChildren,
  attendanceStatuses
}: Props) {
  const { i18n } = useTranslation()

  const [multiselectChildren, setMultiselectChildren] = useState<UUID[] | null>(
    null
  )

  const groupChildren = useMemo(
    () =>
      unitOrGroup.type === 'unit'
        ? unitChildren
        : unitChildren.filter((c) => c.groupId === unitOrGroup.id),
    [unitOrGroup, unitChildren]
  )

  const childrenWithStatus = useCallback(
    (status: AttendanceStatus) =>
      groupChildren.filter(
        (child) =>
          childAttendanceStatus(child, attendanceStatuses).status === status
      ),
    [groupChildren, attendanceStatuses]
  )

  const {
    totalAttendances,
    totalComing,
    totalPresent,
    totalDeparted,
    totalAbsent
  } = useMemo(
    () => ({
      totalAttendances: groupChildren.length,
      totalComing: childrenWithStatus('COMING').length,
      totalPresent: childrenWithStatus('PRESENT').length,
      totalDeparted: childrenWithStatus('DEPARTED').length,
      totalAbsent: childrenWithStatus('ABSENT').length
    }),
    [childrenWithStatus, groupChildren.length]
  )

  const tabs = useMemo(() => {
    const getLabel = (title: string, count: number) => (
      <>
        {title}
        <br />
        <span data-qa="count">{count}</span>/
        <span data-qa="total">{totalAttendances}</span>
      </>
    )

    return [
      {
        id: 'coming',
        link: routes.childAttendanceListState(unitOrGroup, 'coming'),
        label: getLabel(i18n.attendances.types.COMING, totalComing)
      },
      {
        id: 'present',
        link: routes.childAttendanceListState(unitOrGroup, 'present'),
        label: getLabel(i18n.attendances.types.PRESENT, totalPresent)
      },
      {
        id: 'departed',
        link: routes.childAttendanceListState(unitOrGroup, 'departed'),
        label: getLabel(i18n.attendances.types.DEPARTED, totalDeparted)
      },
      {
        id: 'absent',
        link: routes.childAttendanceListState(unitOrGroup, 'absent'),
        label: getLabel(i18n.attendances.types.ABSENT, totalAbsent)
      }
    ]
  }, [
    unitOrGroup,
    i18n,
    totalComing,
    totalAttendances,
    totalPresent,
    totalDeparted,
    totalAbsent
  ])

  const [sortType, setSortType] = useState<SortType>('CHILD_FIRST_NAME')
  const filteredChildren: ListItem[] = useMemo(
    () =>
      sortBy(
        childrenWithStatus(activeStatus).map((child) => ({
          ...child,
          status: activeStatus
        })),
        sortTypeFn(sortType)
      ),
    [activeStatus, childrenWithStatus, sortType]
  )

  // this resetting should be done as a tab change side effect but
  // that would require changing tabs from NavLinks to buttons
  useEffect(() => {
    setMultiselectChildren(null)
    setSortType('CHILD_FIRST_NAME')
  }, [activeStatus])

  return (
    <>
      <TabLinks tabs={tabs} mobile sticky topOffset={64} />
      <ContentArea
        opaque={false}
        paddingVertical="zero"
        paddingHorizontal="zero"
      >
        <ChildList
          unitOrGroup={unitOrGroup}
          items={filteredChildren}
          type={activeStatus}
          multiselectChildren={multiselectChildren}
          setMultiselectChildren={setMultiselectChildren}
          selectedSortType={sortType}
          setSelectedSortType={setSortType}
        />
      </ContentArea>
    </>
  )
})

const maxTime = LocalTime.MAX.format()
const sortTypeFn = (sortType: SortType) => (child: AttendanceChild) => {
  switch (sortType) {
    case 'CHILD_FIRST_NAME':
      return child.firstName
    case 'RESERVATION_START_TIME': {
      const now = HelsinkiDateTime.now()
      const reservationTime = child.reservations.reduce<string | undefined>(
        (min, reservation) => {
          switch (reservation.type) {
            case 'NO_TIMES':
              return min
            case 'TIMES': {
              const end = reservation.range.end
              if (end.isBefore(new TimeRangeEndpoint.End(now.toLocalTime())))
                return min
              const start = reservation.range.start.format()
              return min === undefined || start < min ? start : min
            }
          }
        },
        undefined
      )
      const dailyServiceTime = getServiceTimeRangeOrNullForDate(
        child.dailyServiceTimes,
        now.toLocalDate()
      )?.start?.format()
      return [reservationTime ?? dailyServiceTime ?? maxTime, child.firstName]
    }
    case 'RESERVATION_END_TIME': {
      const now = HelsinkiDateTime.now()
      const reservationTime = child.reservations.reduce<string | undefined>(
        (max, reservation) => {
          switch (reservation.type) {
            case 'NO_TIMES':
              return max
            case 'TIMES': {
              const start = reservation.range.start
              if (start.isAfter(new TimeRangeEndpoint.End(now.toLocalTime())))
                return max
              const end = reservation.range.end.format()
              return max === undefined || end > max ? end : max
            }
          }
        },
        undefined
      )
      const dailyServiceTime = getServiceTimeRangeOrNullForDate(
        child.dailyServiceTimes,
        now.toLocalDate()
      )?.end?.format()
      return [reservationTime ?? dailyServiceTime ?? maxTime, child.firstName]
    }
  }
}
