// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { animated, useSpring } from '@react-spring/web'
import React, { useCallback, useState } from 'react'
import styled, { css } from 'styled-components'

import { useCloseOnOutsideEvent } from 'lib-common/utils/useCloseOnOutsideEvent'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { fontSizesMobile, fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faChevronDown, farUsers, faSearch } from 'lib-icons'

import { zIndex } from '../constants'

import type { GroupSelectorProps } from './GroupSelector'
import GroupSelector, { IconSlot, listMaxHeightVh } from './GroupSelector'
import { useTranslation } from './i18n'

const GroupContainer = styled.div`
  position: relative;
  padding: ${defaultMargins.s};
`

const roundedBox = css`
  background-color: ${colors.grayscale.g0};
  border: 1px solid ${colors.grayscale.g15};
  border-radius: 24px;
`

const SelectorPill = styled.div`
  ${roundedBox};
  display: flex;
  align-items: center;
  height: 48px;
  padding: 0 ${defaultMargins.s};
`

const GroupSelectorButton = styled.button`
  display: flex;
  align-items: center;
  flex-grow: 1;
  min-width: 0;
  gap: ${defaultMargins.s};
  height: 100%;
  border: none;
  background: transparent;
  color: ${colors.main.m2};
  font-family: Montserrat, sans-serif;
  font-size: ${fontSizesMobile.h3};
  font-weight: ${fontWeights.semibold};
  padding: 0;
  cursor: pointer;
  outline: none;
`

const GroupName = styled.span`
  flex-grow: 1;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`

const PillDivider = styled.div`
  flex-shrink: 0;
  width: 1px;
  height: 24px;
  background-color: ${colors.grayscale.g15};
  margin: 0 ${defaultMargins.s};
`

const DropdownBox = animated(styled.div`
  ${roundedBox};
  box-shadow: 0 2px 4px 0 #0000001a;
  position: absolute;
  top: ${defaultMargins.s};
  left: ${defaultMargins.s};
  right: ${defaultMargins.s};
  overflow: hidden;
  z-index: ${zIndex.groupSelector};
`)

export interface Props extends GroupSelectorProps {
  onSearch?: () => void
}

export const GroupSelectorBar = React.memo(function GroupSelectorBar({
  unitId,
  selectedGroup,
  onChangeGroup,
  onSearch,
  countInfo,
  groups,
  includeSelectAll,
  shiftCareSelected,
  onSelectShiftCare
}: Props) {
  const { i18n } = useTranslation()
  const [showGroupSelector, setShowGroupSelector] = useState(false)
  const containerRef = useCloseOnOutsideEvent(
    showGroupSelector,
    useCallback(() => setShowGroupSelector(false), [])
  )
  const { x } = useSpring({
    x: showGroupSelector ? 1 : 0,
    config: { duration: 100 }
  })
  const hasMultipleGroups = groups && groups.length > 1
  const selectedName = shiftCareSelected
    ? i18n.common.shiftCare
    : selectedGroup
      ? selectedGroup.name
      : i18n.common.allGroups
  return (
    <GroupContainer
      ref={containerRef}
      data-qa={`selected-group--${shiftCareSelected ? 'shift-care' : (selectedGroup?.id ?? 'all')}`}
    >
      <SelectorPill>
        <GroupSelectorButton
          onClick={() => setShowGroupSelector((show) => !show)}
          aria-expanded={showGroupSelector}
          data-qa="group-selector-button"
        >
          <IconSlot>
            <FontAwesomeIcon icon={farUsers} />
          </IconSlot>
          <GroupName>{selectedName}</GroupName>
          {hasMultipleGroups && <FontAwesomeIcon icon={faChevronDown} />}
        </GroupSelectorButton>
        {onSearch && (
          <>
            <PillDivider />
            <IconOnlyButton
              onClick={onSearch}
              icon={faSearch}
              aria-label={i18n.common.search}
              data-qa="search-button"
            />
          </>
        )}
      </SelectorPill>
      <DropdownBox
        style={{
          opacity: x,
          maxHeight: x.to((v) => `${v * listMaxHeightVh}vh`),
          visibility: x.to((v) => (v === 0 ? 'hidden' : 'visible')),
          pointerEvents: showGroupSelector ? 'auto' : 'none'
        }}
      >
        <GroupSelector
          unitId={unitId}
          selectedGroup={shiftCareSelected ? undefined : selectedGroup}
          onChangeGroup={(group) => {
            onChangeGroup(group)
            setShowGroupSelector(false)
          }}
          countInfo={countInfo}
          groups={groups}
          includeSelectAll={includeSelectAll}
          shiftCareSelected={shiftCareSelected}
          onSelectShiftCare={
            onSelectShiftCare
              ? () => {
                  onSelectShiftCare()
                  setShowGroupSelector(false)
                }
              : undefined
          }
        />
      </DropdownBox>
    </GroupContainer>
  )
})
