// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import {
  AttendanceChild,
  AttendanceStatus
} from 'lib-common/generated/api-types/attendance'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import {
  defaultMargins,
  isSpacingSize,
  SpacingSize
} from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { routes } from '../App'
import { useTranslation } from '../common/i18n'
import { UnitOrGroup } from '../common/unit-or-group'

import ChildListItem from './ChildListItem'

export interface ListItem extends AttendanceChild {
  status: AttendanceStatus
}

interface Props {
  unitOrGroup: UnitOrGroup
  items: ListItem[]
  type?: AttendanceStatus
}

const NoChildrenOnList = styled.div`
  text-align: center;
  margin-top: 40px;
`

export default React.memo(function ChildList({
  unitOrGroup,
  items,
  type
}: Props) {
  const { i18n } = useTranslation()
  const unitId = unitOrGroup.unitId

  return (
    <FixedSpaceColumn>
      <OrderedList spacing="zero">
        {items.length > 0 ? (
          items.map((ac) => (
            <Li key={ac.id}>
              <ChildListItem
                unitOrGroup={unitOrGroup}
                type={type}
                key={ac.id}
                child={ac}
                childAttendanceUrl={routes.child(unitId, ac.id).value}
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

// eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
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
