// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useMemo, useState } from 'react'

import {
  AttendanceChild,
  AttendanceStatus
} from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'
import { ContentArea } from 'lib-components/layout/Container'
import { TabLinks } from 'lib-components/molecules/Tabs'

import { routes } from '../App'
import { useTranslation } from '../common/i18n'
import { UnitOrGroup } from '../common/unit-or-group'

import ChildList, { ListItem } from './ChildList'
import { AttendanceStatuses, childAttendanceStatus } from './utils'

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

  const filteredChildren: ListItem[] = useMemo(
    () =>
      childrenWithStatus(activeStatus).map((child) => ({
        ...child,
        status: activeStatus
      })),
    [activeStatus, childrenWithStatus]
  )

  // this resetting should be done as a tab change side effect but
  // that would require changing tabs from NavLinks to buttons
  useEffect(() => {
    setMultiselectChildren(null)
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
        />
      </ContentArea>
    </>
  )
})
