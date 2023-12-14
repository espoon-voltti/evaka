// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'

import {
  AttendanceChild,
  AttendanceStatus
} from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'
import { ContentArea } from 'lib-components/layout/Container'
import { TabLinks } from 'lib-components/molecules/Tabs'
import { featureFlags } from 'lib-customizations/employeeMobile'

import { useTranslation } from '../common/i18n'
import { useSelectedGroup } from '../common/selected-group'

import ChildList, { ListItem } from './ChildList'
import { AttendanceStatuses, childAttendanceStatus } from './utils'

interface Props {
  unitId: UUID
  activeStatus: AttendanceStatus
  unitChildren: AttendanceChild[]
  attendanceStatuses: AttendanceStatuses
}

export default React.memo(function AttendanceList({
  unitId,
  activeStatus,
  unitChildren,
  attendanceStatuses
}: Props) {
  const { i18n } = useTranslation()
  const { groupRoute, selectedGroupId } = useSelectedGroup()

  const groupChildren = useMemo(
    () =>
      selectedGroupId.type === 'all'
        ? unitChildren
        : unitChildren.filter((c) => c.groupId === selectedGroupId.id),
    [selectedGroupId, unitChildren]
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
    const url = `${groupRoute}/child-attendance/list`

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
    groupRoute,
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
      <TabLinks
        tabs={tabs}
        mobile
        sticky
        topOffset={
          featureFlags.employeeMobileConfirmedDaysReservations ? 64 : 0
        }
      />
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
