// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import {
  AttendanceChild,
  AttendanceStatus
} from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'
import { Button } from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  defaultMargins,
  isSpacingSize,
  SpacingSize
} from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTimes } from 'lib-icons'

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
  const navigate = useNavigate()
  const unitId = unitOrGroup.unitId

  const [multiSelectMode, setMultiSelectMode] = useState(false)
  const [selectedChildren, setSelectedChildren] = useState<UUID[]>([])

  const enterMultiSelectMode = () => {
    setMultiSelectMode(true)
    setSelectedChildren([])
  }

  const exitMultiSelectMode = () => {
    setMultiSelectMode(false)
    setSelectedChildren([])
  }

  useEffect(() => {
    if (type !== 'COMING') exitMultiSelectMode()
  }, [type])

  return (
    <>
      <FixedSpaceColumn>
        <OrderedList spacing="zero">
          {items.length > 0 ? (
            <>
              {type === 'COMING' && (
                <Li>
                  <MultiselectToggleBox>
                    <Checkbox
                      checked={multiSelectMode}
                      onChange={(checked) =>
                        checked ? enterMultiSelectMode() : exitMultiSelectMode()
                      }
                      label={i18n.attendances.actions.arrivalMultiselect.toggle}
                    />
                  </MultiselectToggleBox>
                </Li>
              )}
              {items.map((ac) => (
                <Li key={ac.id}>
                  <ChildListItem
                    unitOrGroup={unitOrGroup}
                    type={type}
                    key={ac.id}
                    child={ac}
                    childAttendanceUrl={routes.child(unitId, ac.id).value}
                    selected={
                      multiSelectMode ? selectedChildren.includes(ac.id) : null
                    }
                    onChangeSelected={(selected) => {
                      setSelectedChildren((prev) =>
                        selected
                          ? [...prev, ac.id]
                          : prev.filter((id) => id !== ac.id)
                      )
                    }}
                  />
                </Li>
              ))}
            </>
          ) : (
            <NoChildrenOnList data-qa="no-children-indicator">
              {i18n.mobile.emptyList(type || 'ABSENT')}
            </NoChildrenOnList>
          )}
        </OrderedList>
      </FixedSpaceColumn>
      {multiSelectMode && (
        <MultiselectActions>
          <FixedSpaceRow
            spacing="s"
            alignItems="center"
            justifyContent="space-evenly"
          >
            <Button
              appearance="inline"
              text={i18n.common.cancel}
              icon={faTimes}
              onClick={exitMultiSelectMode}
            />
            <FloatingActionButton
              appearance="button"
              primary
              text={i18n.attendances.actions.arrivalMultiselect.confirm(
                selectedChildren.length
              )}
              disabled={selectedChildren.length === 0}
              onClick={() =>
                navigate(routes.markPresent(unitId, selectedChildren).value)
              }
            />
          </FixedSpaceRow>
        </MultiselectActions>
      )}
    </>
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

const MultiselectToggleBox = styled.div`
  align-items: center;
  display: flex;
  padding: ${defaultMargins.s} ${defaultMargins.m};
  border-radius: 2px;
  background-color: ${colors.grayscale.g0};
`

const MultiselectActions = styled.div`
  position: sticky;
  z-index: 10;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100%;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  min-height: 30px;
  background-color: rgba(255, 255, 255, 0.9);
`

const FloatingActionButton = styled(Button)`
  border-radius: 40px;
  max-width: 240px;
  white-space: break-spaces;
`
