// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'

import {
  AttendanceStatus,
  Child
} from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'
import { ContentArea } from 'lib-components/layout/Container'
import Tabs from 'lib-components/molecules/Tabs'

import { useTranslation } from '../common/i18n'

import ChildList from './ChildList'
import { ChildAttendanceStatuses } from './state'

interface Props {
  unitId: UUID
  groupId: UUID | 'all'
  attendanceStatus: AttendanceStatus
  unitChildren: Child[]
  childAttendanceStatuses: ChildAttendanceStatuses
}

export default React.memo(function AttendanceList({
  unitId,
  groupId,
  attendanceStatus,
  unitChildren,
  childAttendanceStatuses
}: Props) {
  const { i18n } = useTranslation()

  const groupChildren = useMemo(
    () =>
      groupId === 'all'
        ? unitChildren
        : unitChildren.filter((c) => c.groupId === groupId),
    [groupId, unitChildren]
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
      totalComing: groupChildren.filter(
        (ac) => childAttendanceStatuses.forChild(ac.id).status === 'COMING'
      ).length,
      totalPresent: groupChildren.filter(
        (ac) => childAttendanceStatuses.forChild(ac.id).status === 'PRESENT'
      ).length,
      totalDeparted: groupChildren.filter(
        (ac) => childAttendanceStatuses.forChild(ac.id).status === 'DEPARTED'
      ).length,
      totalAbsent: groupChildren.filter(
        (ac) => childAttendanceStatuses.forChild(ac.id).status === 'ABSENT'
      ).length
    }),
    [childAttendanceStatuses, groupChildren]
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

  const filteredChildren = useMemo(
    () =>
      groupChildren.filter(
        (child) =>
          childAttendanceStatuses.forChild(child.id).status === attendanceStatus
      ),
    [attendanceStatus, childAttendanceStatuses, groupChildren]
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
          attendanceChildren={filteredChildren}
          childAttendanceStatuses={childAttendanceStatuses}
          type={attendanceStatus}
        />
      </ContentArea>
    </>
  )
})
