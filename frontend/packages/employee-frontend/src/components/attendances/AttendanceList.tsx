// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useParams } from 'react-router-dom'

import { isSuccess, Result } from '~api'
import { AttendanceStatus, ChildInGroup } from '~api/unit'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'
import ChildListItem from './ChildListItem'
import { UnorderedList } from '~components/common/styled/common'
import { UUID } from '~types'

interface Props {
  groupAttendances: Result<ChildInGroup[]>
  type: AttendanceStatus
}

export default React.memo(function AttendanceList({
  groupAttendances,
  type
}: Props) {
  const { id } = useParams<{
    id: UUID
    groupid: UUID | 'all'
  }>()

  return (
    <FixedSpaceColumn>
      <UnorderedList>
        <FixedSpaceColumn spacing={'xs'}>
          {isSuccess(groupAttendances) &&
            groupAttendances.data
              .filter((groupAttendance) => groupAttendance.status === type)
              .map((groupAttendance) => (
                <li key={groupAttendance.childId}>
                  <a
                    href={`/units/${id}/groups/${groupAttendance.daycareGroupId}/childattendance/${groupAttendance.childId}`}
                  >
                    <ChildListItem
                      type={type}
                      key={groupAttendance.childId}
                      childInGroup={groupAttendance}
                    />
                  </a>
                </li>
              ))}
        </FixedSpaceColumn>
      </UnorderedList>
    </FixedSpaceColumn>
  )
})
