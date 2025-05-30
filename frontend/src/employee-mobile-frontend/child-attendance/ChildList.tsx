// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback } from 'react'
import styled from 'styled-components'
import { useLocation } from 'wouter'

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
import { faBarsSort, faTimes } from 'lib-icons'

import { routes } from '../App'
import { useTranslation } from '../common/i18n'
import type { UnitOrGroup } from '../common/unit-or-group'

import ChildListItem from './ChildListItem'

export interface ListItem extends AttendanceChild {
  status: AttendanceStatus
}

export type SortType =
  | 'CHILD_FIRST_NAME'
  | 'RESERVATION_START_TIME'
  | 'RESERVATION_END_TIME'

interface Props {
  unitOrGroup: UnitOrGroup
  items: ListItem[]
  type?: AttendanceStatus
  multiselectChildren: UUID[] | null
  setMultiselectChildren: (selected: UUID[] | null) => void
  selectedSortType: SortType
  setSelectedSortType: (sortType: SortType) => void
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
  setMultiselectChildren,
  selectedSortType,
  setSelectedSortType
}: Props) {
  const { i18n } = useTranslation()
  const [, navigate] = useLocation()
  const unitId = unitOrGroup.unitId

  const getSortOptions = useCallback(() => {
    const baseOptions = [
      {
        value: 'CHILD_FIRST_NAME' as SortType,
        label: i18n.attendances.actions.sortType.CHILD_FIRST_NAME
      }
    ]

    if (type === 'COMING') {
      baseOptions.push({
        value: 'RESERVATION_START_TIME' as SortType,
        label: i18n.attendances.actions.sortType.RESERVATION_START_TIME
      })
    }

    if (type === 'PRESENT') {
      baseOptions.push({
        value: 'RESERVATION_END_TIME' as SortType,
        label: i18n.attendances.actions.sortType.RESERVATION_END_TIME
      })
    }

    return baseOptions
  }, [type, i18n])

  return (
    <>
      <FixedSpaceColumn>
        <OrderedList spacing="zero">
          {items.length > 0 ? (
            <>
              {(type === 'COMING' || type === 'PRESENT') && (
                <Li>
                  <MultiselectToggleBox>
                    <FixedSpaceRow
                      fullWidth
                      alignItems="baseline"
                      justifyContent="space-between"
                    >
                      {type === 'COMING' ||
                      featureFlags.multiSelectDeparture ? (
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
                      ) : (
                        <div />
                      )}
                      <SortSelectWrapper>
                        <SortSelect
                          value={selectedSortType}
                          onChange={(e) =>
                            setSelectedSortType(e.target.value as SortType)
                          }
                          data-qa="sort-type-select"
                          aria-label={i18n.common.sort}
                        >
                          {getSortOptions().map((option) => (
                            <option key={option.value} value={option.value}>
                              {option.label}
                            </option>
                          ))}
                        </SortSelect>
                        <SortIcon>
                          <FontAwesomeIcon icon={faBarsSort} />
                        </SortIcon>
                      </SortSelectWrapper>
                    </FixedSpaceRow>
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

const SortSelectWrapper = styled.div`
  position: relative;
  display: inline-block;
`

const SortSelect = styled.select`
  appearance: none;
  background: transparent;
  border: none;
  color: transparent;
  cursor: pointer;
  font-size: 0;
  height: 40px;
  width: 40px;
  position: absolute;
  top: 0;
  left: 0;
  z-index: 2;

  &:focus {
    outline: 2px solid ${colors.main.m2};
    outline-offset: 2px;
    border-radius: 4px;
  }
`

const SortIcon = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: ${colors.grayscale.g70};
  pointer-events: none;
  position: relative;
  z-index: 1;
`
