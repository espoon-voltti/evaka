// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AttendanceResponse,
  AttendanceStatus
} from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'
import { ContentArea } from 'lib-components/layout/Container'
import Tabs from 'lib-components/molecules/Tabs'
import { Bold } from 'lib-components/typography'
import React, { useMemo } from 'react'
import { useTranslation } from '../../state/i18n'
import ChildList from './ChildList'

interface Props {
  unitId: UUID
  groupId: UUID | 'all'
  attendanceStatus: AttendanceStatus
  attendanceResponse: AttendanceResponse
}

export default React.memo(function AttendanceList({
  unitId,
  groupId,
  attendanceStatus,
  attendanceResponse
}: Props) {
  const { i18n } = useTranslation()

  const groupChildren = useMemo(
    () =>
      groupId === 'all'
        ? attendanceResponse.children
        : attendanceResponse.children.filter((c) => c.groupId === groupId),
    [groupId, attendanceResponse]
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
      totalComing: groupChildren.filter((ac) => ac.status === 'COMING').length,
      totalPresent: groupChildren.filter((ac) => ac.status === 'PRESENT')
        .length,
      totalDeparted: groupChildren.filter((ac) => ac.status === 'DEPARTED')
        .length,
      totalAbsent: groupChildren.filter((ac) => ac.status === 'ABSENT').length
    }),
    [groupChildren]
  )

  const tabs = useMemo(() => {
    const url = `/units/${unitId}/groups/${groupId}/child-attendance/list`

    const getLabel = (title: string, count: number) => (
      <Bold>
        {title}
        <br />
        <span data-qa="count">{count}</span>/
        <span data-qa="total">{totalAttendances}</span>
      </Bold>
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
    () => groupChildren.filter(({ status }) => status === attendanceStatus),
    [attendanceStatus, groupChildren]
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
          groupsNotes={attendanceResponse.groupNotes}
          type={attendanceStatus}
        />
      </ContentArea>
    </>
  )
})
