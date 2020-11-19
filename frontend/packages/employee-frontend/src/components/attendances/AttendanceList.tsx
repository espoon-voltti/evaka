// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useParams } from 'react-router-dom'

import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'
import ChildListItem from './ChildListItem'
import { UnorderedList } from '~components/common/styled/common'
import { UUID } from '~types'
import { AttendanceChild, AttendanceStatus } from '~api/attendances'

interface Props {
  attendanceChildren: AttendanceChild[]
  type?: AttendanceStatus
}

export default React.memo(function AttendanceList({
  attendanceChildren,
  type
}: Props) {
  const { unitId, groupId: groupIdOrAll } = useParams<{
    unitId: UUID
    groupId: UUID | 'all'
  }>()

  if (type) {
    attendanceChildren = attendanceChildren.filter((ac) => ac.status === type)
  }

  return (
    <FixedSpaceColumn>
      <UnorderedList spacing={'xs'}>
        {attendanceChildren.map((ac) => (
          <li key={ac.id}>
            <a
              href={`/employee/units/${unitId}/groups/${groupIdOrAll}/childattendance/${ac.id}`}
            >
              <ChildListItem
                type={ac.status}
                key={ac.id}
                attendanceChild={ac}
              />
            </a>
          </li>
        ))}
      </UnorderedList>
    </FixedSpaceColumn>
  )
})
