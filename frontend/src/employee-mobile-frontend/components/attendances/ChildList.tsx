// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import {
  AttendanceStatus,
  Child
} from 'lib-common/generated/api-types/attendance'
import { GroupNote } from 'lib-common/generated/api-types/note'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import {
  defaultMargins,
  isSpacingSize,
  SpacingSize
} from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'

import ChildListItem from './ChildListItem'

interface Props {
  unitId: string
  attendanceChildren: Child[]
  groupsNotes: GroupNote[]
  type?: AttendanceStatus
}

const NoChildrenOnList = styled.div`
  text-align: center;
  margin-top: 40px;
`

export default React.memo(function ChildList({
  unitId,
  attendanceChildren,
  type,
  groupsNotes
}: Props) {
  const { i18n } = useTranslation()

  const getGroupNote = (child: Child): GroupNote | undefined =>
    groupsNotes.find(({ groupId }) => groupId == child.groupId)

  return (
    <FixedSpaceColumn>
      <OrderedList spacing="zero">
        {attendanceChildren.length > 0 ? (
          attendanceChildren.map((ac) => (
            <Li key={ac.id}>
              <ChildListItem
                type={type}
                key={ac.id}
                child={ac}
                groupNote={getGroupNote(ac)}
                childAttendanceUrl={`/units/${unitId}/groups/${ac.groupId}/child-attendance/${ac.id}`}
              />
            </Li>
          ))
        ) : (
          <NoChildrenOnList data-qa="no-children-indicator">
            {i18n.mobile.emptyList(type || 'ABSENT')}
          </NoChildrenOnList>
        )}
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
    background: ${colors.grayscale.g15};
    height: 1px;
    display: block;
    position: absolute;
    left: ${defaultMargins.s};
  }
`
