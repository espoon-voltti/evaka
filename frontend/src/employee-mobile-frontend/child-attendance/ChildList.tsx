// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useMemo } from 'react'
import styled from 'styled-components'
import { useLocation } from 'wouter'

import type {
  AttendanceChild,
  AttendanceStatus
} from 'lib-common/generated/api-types/attendance'
import type { UUID } from 'lib-common/types'
import { Button } from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employeeMobile'
import { faBarsSort, faTimes } from 'lib-icons'

import { routes } from '../App'
import { bottomNavBarHeight } from '../common/BottomNavbar'
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

  const sortOptions = useMemo(() => {
    const options: { value: SortType; label: string }[] = [
      {
        value: 'CHILD_FIRST_NAME',
        label: i18n.attendances.actions.sortType.CHILD_FIRST_NAME
      }
    ]
    if (type === 'COMING') {
      options.push({
        value: 'RESERVATION_START_TIME',
        label: i18n.attendances.actions.sortType.RESERVATION_START_TIME
      })
    }
    if (type === 'PRESENT') {
      options.push({
        value: 'RESERVATION_END_TIME',
        label: i18n.attendances.actions.sortType.RESERVATION_END_TIME
      })
    }
    return options
  }, [type, i18n])

  const multiselectAction =
    type === 'COMING'
      ? {
          confirmText: i18n.attendances.actions.multiselect.confirmArrival,
          confirmRoute: (children: UUID[]) =>
            routes.markPresent(unitId, children, true),
          dataQa: 'mark-multiple-arrived'
        }
      : type === 'PRESENT'
        ? {
            confirmText: i18n.attendances.actions.multiselect.confirmDeparture,
            confirmRoute: (children: UUID[]) =>
              routes.markDeparted(unitId, children, true),
            dataQa: 'mark-multiple-departed'
          }
        : undefined

  return (
    <>
      {items.length > 0 ? (
        <>
          {(type === 'COMING' || type === 'PRESENT') && (
            <MultiselectToggleBox>
              {type === 'COMING' || featureFlags.multiSelectDeparture ? (
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
              ) : null}
              <SortSelectWrapper>
                <SortSelect
                  value={selectedSortType}
                  onChange={(e) =>
                    setSelectedSortType(e.target.value as SortType)
                  }
                  data-qa="sort-type-select"
                  aria-label={i18n.common.sort}
                >
                  {sortOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </SortSelect>
                <SortIcon>
                  <FontAwesomeIcon icon={faBarsSort} />
                </SortIcon>
              </SortSelectWrapper>
            </MultiselectToggleBox>
          )}
          <OrderedList>
            {items.map((ac) => (
              <li key={ac.id}>
                <ChildListItem
                  unitOrGroup={unitOrGroup}
                  type={type}
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
              </li>
            ))}
          </OrderedList>
        </>
      ) : (
        <NoChildrenOnList data-qa="no-children-indicator">
          {i18n.mobile.emptyList(type ?? 'ABSENT')}
        </NoChildrenOnList>
      )}
      {multiselectChildren && (
        <MultiselectActions>
          <FixedSpaceRow
            $spacing="s"
            $alignItems="center"
            $justifyContent="space-evenly"
          >
            <Button
              appearance="inline"
              text={i18n.common.cancel}
              icon={faTimes}
              onClick={() => setMultiselectChildren(null)}
            />
            {multiselectAction && (
              <FloatingActionButton
                appearance="button"
                primary
                text={multiselectAction.confirmText(multiselectChildren.length)}
                disabled={multiselectChildren.length === 0}
                onClick={() =>
                  navigate(
                    multiselectAction.confirmRoute(multiselectChildren).value
                  )
                }
                data-qa={multiselectAction.dataQa}
              />
            )}
          </FixedSpaceRow>
        </MultiselectActions>
      )}
    </>
  )
})

const NoChildrenOnList = styled.div`
  text-align: center;
  margin-top: 40px;
`

const OrderedList = styled.ol`
  list-style: none;
  padding: 0;
  margin: 0;

  li {
    border-bottom: 1px solid ${colors.grayscale.g15};
  }
`

const MultiselectToggleBox = styled.div`
  display: flex;
  background-color: ${colors.grayscale.g0};
  padding: ${defaultMargins.s} ${defaultMargins.m};
  border-bottom: 1px solid ${colors.grayscale.g15};
`

const MultiselectActions = styled.div`
  position: sticky;
  z-index: 10;
  bottom: ${bottomNavBarHeight}px;
  left: 0;
  right: 0;
  width: 100%;
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  min-height: 30px;
  background-color: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid ${colors.grayscale.g15};
  border-top: 1px solid ${colors.grayscale.g15};
  margin-top: -1px;
`

const FloatingActionButton = styled(Button)`
  border-radius: 40px;
  max-width: 240px;
  white-space: break-spaces;
`

const SortSelectWrapper = styled.div`
  position: relative;
  display: inline-block;
  margin-left: auto;
`

const SortSelect = styled.select`
  appearance: none;
  background: transparent;
  border: none;
  color: transparent;
  cursor: pointer;
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
