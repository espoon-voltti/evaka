// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'

import {
  AttendanceStatus,
  AttendanceChild
} from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'
import { ContentArea } from 'lib-components/layout/Container'
import Tabs from 'lib-components/molecules/Tabs'

import { useTranslation } from '../common/i18n'

import ChildList, { ListItem } from './ChildList'
import { AttendanceStatuses, childAttendanceStatus } from './utils'

interface Props {
  unitId: UUID
  // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
  groupId: UUID | 'all'
  activeStatus: AttendanceStatus
  unitChildren: AttendanceChild[]
  attendanceStatuses: AttendanceStatuses
}

export default React.memo(function AttendanceList({
  unitId,
  groupId,
  activeStatus,
  unitChildren,
  attendanceStatuses
}: Props) {
  const { i18n } = useTranslation()

  const groupChildren = useMemo(
    () =>
      groupId === 'all'
        ? unitChildren
        : unitChildren.filter((c) => c.groupId === groupId),
    [groupId, unitChildren]
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
    const url = `/units/${unitId}/groups/${groupId}/child-attendance/list`

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
        link: `${url}/coming`,
        label: getLabel(i18n.attendances.types.COMING, totalComing)
      },
      {
        id: 'present',
        link: `${url}/present`,
        label: getLabel(i18n.attendances.types.PRESENT, totalPresent)
      },
      {
        id: 'departed',
        link: `${url}/departed`,
        label: getLabel(i18n.attendances.types.DEPARTED, totalDeparted)
      },
      {
        id: 'absent',
        link: `${url}/absent`,
        label: getLabel(i18n.attendances.types.ABSENT, totalAbsent)
      }
    ]
  }, [
    unitId,
    groupId,
    i18n,
    totalComing,
    totalAttendances,
    totalPresent,
    totalDeparted,
    totalAbsent
  ])

  const filteredChildren: ListItem[] = useMemo(
    () =>
      childrenWithStatus(activeStatus).map((child) => ({
        ...child,
        status: activeStatus
      })),
    [activeStatus, childrenWithStatus]
  )

  return (
    <>
      <Tabs tabs={tabs} mobile />
      <ContentArea
        opaque={false}
        paddingVertical="zero"
        paddingHorizontal="zero"
      >
        <ChildList
          unitId={unitId}
          items={filteredChildren}
          type={activeStatus}
        />
      </ContentArea>
    </>
  )
})
