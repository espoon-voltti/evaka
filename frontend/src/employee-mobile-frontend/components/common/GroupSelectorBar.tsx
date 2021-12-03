// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faAngleDown, faAngleUp, faSearch } from 'lib-icons'
import React, { useState } from 'react'
import { animated, useSpring } from 'react-spring'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'
import { zIndex } from '../constants'
import GroupSelector, { CountInfo } from './GroupSelector'

const GroupContainer = styled.div`
  display: block;
  min-height: 48px;
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
  background-color: ${colors.greyscale.white};
  box-shadow: 0 2px 6px 0 ${colors.greyscale.lighter};
  position: absolute;
  display: flex;
  flex-direction: column;
  overflow-y: hidden;
  min-height: 48px;
  width: 100%;
  z-index: ${zIndex.groupSelector};
`)

export interface Props {
  selectedGroup: GroupInfo | undefined
  onChangeGroup: (group: GroupInfo | undefined) => void
  onSearch?: () => void
  countInfo?: CountInfo
  groups?: GroupInfo[]
}

export const GroupSelectorBar = React.memo(function GroupSelectorBar({
  selectedGroup,
  onChangeGroup,
  onSearch,
  countInfo,
  groups
}: Props) {
  const { i18n } = useTranslation()
  const [showGroupSelector, setShowGroupSelector] = useState<boolean>(false)
  const groupSelectorSpring = useSpring<{ x: number }>({
    x: showGroupSelector ? 1 : 0,
    config: { duration: 100 }
  })
  return (
    <GroupContainer data-qa={`selected-group--${selectedGroup?.id ?? 'all'}`}>
      <GroupSelectorWrapper
        style={{
          maxHeight: groupSelectorSpring.x.interpolate((x) => `${100 * x}%`)
        }}
      >
        <GroupSelectorButtonRow>
          <GroupSelectorButton
            text={selectedGroup ? selectedGroup.name : i18n.common.all}
            onClick={() => {
              setShowGroupSelector(!showGroupSelector)
            }}
            icon={showGroupSelector ? faAngleUp : faAngleDown}
            iconRight
            data-qa="group-selector-button"
          />
          {onSearch && <IconButton onClick={onSearch} icon={faSearch} />}
        </GroupSelectorButtonRow>
        <GroupSelector
          selectedGroup={selectedGroup}
          onChangeGroup={(group) => {
            onChangeGroup(group)
            setShowGroupSelector(false)
          }}
          countInfo={countInfo}
          groups={groups}
          data-qa="group-selector"
        />
      </GroupSelectorWrapper>
      <Gap size="s" />
    </GroupContainer>
  )
})
