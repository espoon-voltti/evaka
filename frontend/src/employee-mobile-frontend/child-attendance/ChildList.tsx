// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import type {
  AttendanceChild,
  AttendanceStatus
} from 'lib-common/generated/api-types/attendance'
import type { UUID } from 'lib-common/types'
import { Button } from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import type { SpacingSize } from 'lib-components/white-space'
import { defaultMargins, isSpacingSize } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employeeMobile'
import { faTimes } from 'lib-icons'

import { routes } from '../App'
import { useTranslation } from '../common/i18n'
import type { UnitOrGroup } from '../common/unit-or-group'

import ChildListItem from './ChildListItem'

export interface ListItem extends AttendanceChild {
  status: AttendanceStatus
}

interface Props {
  unitOrGroup: UnitOrGroup
  items: ListItem[]
  type?: AttendanceStatus
  multiselectChildren: UUID[] | null
  setMultiselectChildren: (selected: UUID[] | null) => void
}

const NoChildrenOnList = styled.div`
  text-align: center;
  margin-top: 40px;
`

export default React.memo(function ChildList({
  unitOrGroup,
  items,
  type,
  multiselectChildren,
  setMultiselectChildren
}: Props) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const unitId = unitOrGroup.unitId

  return (
    <>
      <FixedSpaceColumn>
        <OrderedList spacing="zero">
          {items.length > 0 ? (
            <>
              {(type === 'COMING' ||
                (type === 'PRESENT' && featureFlags.multiSelectDeparture)) && (
                <Li>
                  <MultiselectToggleBox>
                    <Checkbox
                      checked={multiselectChildren !== null}
                      onChange={(checked) =>
                        checked
                          ? setMultiselectChildren([])
                          : setMultiselectChildren(null)
                      }
                      label={i18n.attendances.actions.multiselect.toggle}
                      data-qa="multiselect-toggle"
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
                      multiselectChildren
                        ? multiselectChildren.includes(ac.id)
                        : null
                    }
                    onChangeSelected={(selected) => {
                      if (multiselectChildren) {
                        setMultiselectChildren(
                          selected
                            ? [...multiselectChildren, ac.id]
                            : multiselectChildren.filter((id) => id !== ac.id)
                        )
                      }
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
      {multiselectChildren && (
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
              onClick={() => setMultiselectChildren(null)}
            />
            {type === 'COMING' && (
              <FloatingActionButton
                appearance="button"
                primary
                text={i18n.attendances.actions.multiselect.confirmArrival(
                  multiselectChildren.length
                )}
                disabled={multiselectChildren.length === 0}
                onClick={() =>
                  navigate(
                    routes.markPresent(unitId, multiselectChildren, true).value
                  )
                }
                data-qa="mark-multiple-arrived"
              />
            )}
            {type === 'PRESENT' && (
              <FloatingActionButton
                appearance="button"
                primary
                text={i18n.attendances.actions.multiselect.confirmDeparture(
                  multiselectChildren.length
                )}
                disabled={multiselectChildren.length === 0}
                onClick={() =>
                  navigate(
                    routes.markDeparted(unitId, multiselectChildren, true).value
                  )
                }
                data-qa="mark-multiple-departed"
              />
            )}
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
