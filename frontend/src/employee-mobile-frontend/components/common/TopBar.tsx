import React, { useState } from 'react'
import styled from 'styled-components'
import { animated, useSpring } from 'react-spring'
import { Gap, defaultMargins } from 'lib-components/white-space'
import { faAngleDown, faAngleUp, faSearch } from 'lib-icons'
import Title from 'lib-components/atoms/Title'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import colors from 'lib-components/colors'
import GroupSelector from './GroupSelector'
import { Group } from '../../api/attendances'
import { useTranslation } from '../../state/i18n'

export interface Props {
  unitName: string
  selectedGroup: Group | undefined
  allowAllGroups?: boolean
  onChangeGroup: (group: Group | undefined) => void
  onSearch?: () => void
}

export default function TopBar({
  unitName,
  selectedGroup,
  allowAllGroups = true,
  onChangeGroup,
  onSearch
}: Props) {
  const { i18n } = useTranslation()
  const [showGroupSelector, setShowGroupSelector] = useState<boolean>(false)
  const groupSelectorSpring = useSpring<{ x: number }>({
    x: showGroupSelector ? 1 : 0,
    config: { duration: 100 }
  })
  return (
    <>
      <Name>
        <NoMarginTitle size={1} centered smaller bold>
          {unitName}
          {onSearch && <IconButton onClick={onSearch} icon={faSearch} />}
        </NoMarginTitle>
      </Name>
      <GroupSelectorWrapper
        style={{
          maxHeight: groupSelectorSpring.x.interpolate((x) => `${100 * x}%`)
        }}
      >
        <GroupSelectorButton
          text={selectedGroup ? selectedGroup.name : i18n.common.all}
          onClick={() => {
            setShowGroupSelector(!showGroupSelector)
          }}
          icon={showGroupSelector ? faAngleUp : faAngleDown}
          iconRight
          data-qa="group-selector-button"
        />
        <GroupSelector
          selectedGroup={selectedGroup}
          allowAllGroups={allowAllGroups}
          onChangeGroup={(group) => {
            onChangeGroup(group)
            setShowGroupSelector(false)
          }}
          data-qa="group-selector"
        />
      </GroupSelectorWrapper>
      <Gap size={'XL'} />
    </>
  )
}

const NoMarginTitle = styled(Title)`
  margin-top: 0;
  margin-bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background: ${colors.blues.primary};
  color: ${colors.greyscale.white};
  box-shadow: 0px 2px 6px 0px ${colors.greyscale.lighter};
  position: relative;
  z-index: 1;

  button {
    margin-left: ${defaultMargins.m};
    color: ${colors.greyscale.white};
  }
`

const Name = styled.div`
  background: ${colors.greyscale.white};
`

const GroupSelectorButton = styled(InlineButton)`
  width: 100%;
  border: none;
  font-family: Montserrat, sans-serif;
  font-size: 20px;
  height: 48px;
  flex-shrink: 0;
`

const GroupSelectorWrapper = animated(styled.div`
  box-shadow: 0px 2px 6px 0px ${colors.greyscale.lighter};
  position: absolute;
  z-index: 1;
  display: flex;
  background-color: ${colors.greyscale.white};
  flex-direction: column;
  overflow-y: hidden;
  min-height: 48px;
  width: 100%;
`)
