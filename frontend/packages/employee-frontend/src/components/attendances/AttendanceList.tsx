// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useParams } from 'react-router-dom'

import { AttendanceStatus, ChildInGroup } from '~api/unit'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'
import ChildListItem from './ChildListItem'
import { UnorderedList } from '~components/common/styled/common'
import { UUID } from '~types'

interface Props {
  groupAttendances: ChildInGroup[]
  type?: AttendanceStatus
}

export default React.memo(function AttendanceList({
  groupAttendances,
  type
}: Props) {
  const { unitId } = useParams<{
    unitId: UUID
  }>()

  if (type) {
    groupAttendances = groupAttendances.filter(
      (groupAttendance) => groupAttendance.status === type
    )
  }

  return (
    <FixedSpaceColumn>
      <UnorderedList spacing={'xs'}>
        {groupAttendances.map((groupAttendance) => (
          <li key={groupAttendance.childId}>
            <a
              href={`/employee/units/${unitId}/groups/${groupAttendance.daycareGroupId}/childattendance/${groupAttendance.childId}`}
            >
              <ChildListItem
                type={groupAttendance.status}
                key={groupAttendance.childId}
                childInGroup={groupAttendance}
              />
            </a>
          </li>
        ))}
      </UnorderedList>
    </FixedSpaceColumn>
  )
})
