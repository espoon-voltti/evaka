// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'

import {
  defaultMargins,
  isSpacingSize,
  SpacingSize
} from '@evaka/lib-components/src/white-space'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import ChildListItem from './ChildListItem'
import { AttendanceChild, AttendanceStatus } from '~api/attendances'
import colors from '@evaka/lib-components/src/colors'

interface Props {
  attendanceChildren: AttendanceChild[]
  type?: AttendanceStatus
  showAll?: boolean
}

export default React.memo(function AttendanceList({
  attendanceChildren,
  type,
  showAll
}: Props) {
  const { unitId, groupId: groupIdOrAll } = useParams<{
    unitId: string
    groupId: string | 'all'
  }>()

  if (type) {
    attendanceChildren = attendanceChildren.filter((ac) => ac.status === type)
  }

  if (groupIdOrAll !== 'all' && !showAll) {
    attendanceChildren = attendanceChildren.filter(
      (ac) => ac.groupId === groupIdOrAll
    )
  }

  return (
    <FixedSpaceColumn>
      <OrderedList spacing={'zero'}>
        {attendanceChildren.map((ac) => (
          <Li key={ac.id}>
            <ChildListItem
              type={ac.status}
              key={ac.id}
              attendanceChild={ac}
              childAttendanceUrl={`/units/${unitId}/groups/${groupIdOrAll}/childattendance/${ac.id}`}
            />
          </Li>
        ))}
      </OrderedList>
    </FixedSpaceColumn>
  )
})

const OrderedList = styled.ol<{ spacing?: SpacingSize | string }>`
  list-style: none;
  padding: 0;
  margin-top: 0;

  li {
    margin-bottom: ${(p) =>
      p.spacing
        ? isSpacingSize(p.spacing)
          ? defaultMargins[p.spacing]
          : p.spacing
        : defaultMargins.s};

    &:last-child {
      margin-bottom: 0;
    }
  }
`

const Li = styled.li`
  &:after {
    content: '';
    width: calc(100% - ${defaultMargins.s});
    background: ${colors.greyscale.lighter};
    height: 1px;
    display: block;
    position: absolute;
    left: ${defaultMargins.s};
  }
`
