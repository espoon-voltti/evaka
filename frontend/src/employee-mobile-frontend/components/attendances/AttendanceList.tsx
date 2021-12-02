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
import { fontWeights } from 'lib-components/typography'
import React, { useMemo } from 'react'
import styled from 'styled-components'
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
    return [
      {
        id: 'coming',
        link: `${url}/coming`,
        label: (
          <Bold>
            {i18n.attendances.types.COMING}
            <br />
            {`${totalComing}/${totalAttendances}`}
          </Bold>
        )
      },
      {
        id: 'present',
        link: `${url}/present`,
        label: (
          <Bold>
            {i18n.attendances.types.PRESENT}
            <br />
            {`${totalPresent}/${totalAttendances}`}
          </Bold>
        )
      },
      {
        id: 'departed',
        link: `${url}/departed`,
        label: (
          <Bold>
            {i18n.attendances.types.DEPARTED}
            <br />
            {`${totalDeparted}/${totalAttendances}`}
          </Bold>
        )
      },
      {
        id: 'absent',
        link: `${url}/absent`,
        label: (
          <Bold>
            {i18n.attendances.types.ABSENT}
            <br />
            {`${totalAbsent}/${totalAttendances}`}
          </Bold>
        )
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

const Bold = styled.div`
  font-weight: ${fontWeights.semibold};
`
