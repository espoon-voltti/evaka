// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { animated, useSpring } from '@react-spring/web'
import React, { useState } from 'react'
import styled from 'styled-components'

import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faAngleDown, faAngleUp, faChevronUp, faSearch } from 'lib-icons'

import { zIndex } from '../constants'

import GroupSelector, { CountInfo } from './GroupSelector'
import { useTranslation } from './i18n'

const GroupContainer = styled.div`
  display: block;
  min-height: 48px;
  margin-bottom: ${defaultMargins.xs};
`

const GroupSelectorButtonRow = styled.div`
  display: flex;
  justify-content: stretch;
  align-items: center;
  padding: 0 ${defaultMargins.s};
`

const GroupSelectorButton = styled(InlineButton)`
  border: none;
  font-family: Montserrat, sans-serif;
  font-size: 20px;
  height: 48px;
  flex-grow: 1;
  flex-shrink: 0;
  outline: none !important;
`

const GroupSelectorWrapper = animated(styled.div`
  background-color: ${colors.grayscale.g0};
  box-shadow: 0 2px 6px 0 ${colors.grayscale.g15};
  position: absolute;
  display: flex;
  flex-direction: column;
  overflow-y: hidden;
  min-height: 48px;
  width: 100%;
  z-index: ${zIndex.groupSelector};
`)

export interface Props {
  unitId: UUID
  selectedGroup: GroupInfo | undefined
  onChangeGroup: (group: GroupInfo | undefined) => void
  onSearch?: () => void
  countInfo?: CountInfo
  groups?: GroupInfo[]
  includeSelectAll: boolean
}

export const GroupSelectorBar = React.memo(function GroupSelectorBar({
  unitId,
  selectedGroup,
  onChangeGroup,
  onSearch,
  countInfo,
  groups,
  includeSelectAll
}: Props) {
  const { i18n } = useTranslation()
  const [showGroupSelector, setShowGroupSelector] = useState<boolean>(false)
  const groupSelectorSpring = useSpring<{ x: number }>({
    x: showGroupSelector ? 1 : 0,
    config: { duration: 100 }
  })
  const hasMultipleGroups = groups && groups.length > 1
  return (
    <GroupContainer data-qa={`selected-group--${selectedGroup?.id ?? 'all'}`}>
      <GroupSelectorWrapper
        style={{
          maxHeight: groupSelectorSpring.x.to((x) => `${100 * x}%`)
        }}
      >
        {!showGroupSelector && (
          <GroupSelectorButtonRow>
            <GroupSelectorButton
              text={selectedGroup ? selectedGroup.name : i18n.common.all}
              onClick={() => setShowGroupSelector(true)}
              icon={
                hasMultipleGroups
                  ? showGroupSelector
                    ? faAngleUp
                    : faAngleDown
                  : undefined
              }
              iconRight
              data-qa="group-selector-button"
            />
            {onSearch && (
              <IconOnlyButton
                onClick={onSearch}
                icon={faSearch}
                aria-label={i18n.common.search}
              />
            )}
          </GroupSelectorButtonRow>
        )}
        <GroupSelector
          unitId={unitId}
          selectedGroup={selectedGroup}
          onChangeGroup={(group) => {
            onChangeGroup(group)
            setShowGroupSelector(false)
          }}
          countInfo={countInfo}
          groups={groups}
          includeSelectAll={includeSelectAll}
          data-qa="group-selector"
        />
        <CloseButtonWrapper>
          <InlineButton
            text={i18n.common.close}
            icon={faChevronUp}
            onClick={() => setShowGroupSelector(false)}
          />
        </CloseButtonWrapper>
      </GroupSelectorWrapper>
      <Gap size="s" />
    </GroupContainer>
  )
})

const CloseButtonWrapper = styled.span`
  text-align: center;
  padding: 19px;
`
