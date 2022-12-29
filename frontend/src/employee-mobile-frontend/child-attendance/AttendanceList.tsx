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
import { AttendanceStatuses, childAttendanceStatus } from './utils'

interface Props {
  unitId: UUID
  groupId: UUID | 'all'
  attendanceStatus: AttendanceStatus
  unitChildren: Child[]
  attendanceStatuses: AttendanceStatuses
}

export default React.memo(function AttendanceList({
  unitId,
  groupId,
  attendanceStatus,
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
        (ac) =>
          childAttendanceStatus(attendanceStatuses, ac.id).status === 'COMING'
      ).length,
      totalPresent: groupChildren.filter(
        (ac) =>
          childAttendanceStatus(attendanceStatuses, ac.id).status === 'PRESENT'
      ).length,
      totalDeparted: groupChildren.filter(
        (ac) =>
          childAttendanceStatus(attendanceStatuses, ac.id).status === 'DEPARTED'
      ).length,
      totalAbsent: groupChildren.filter(
        (ac) =>
          childAttendanceStatus(attendanceStatuses, ac.id).status === 'ABSENT'
      ).length
    }),
    [attendanceStatuses, groupChildren]
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
          childAttendanceStatus(attendanceStatuses, child.id).status ===
          attendanceStatus
      ),
    [attendanceStatus, attendanceStatuses, groupChildren]
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
          attendanceStatuses={attendanceStatuses}
          type={attendanceStatus}
        />
      </ContentArea>
    </>
  )
})
