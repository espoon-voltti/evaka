// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import colors from 'lib-components/colors'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { faChild, faComments, faUser } from 'lib-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'
import { formatDecimal } from 'lib-common/utils/number'

export type NavItem = 'child' | 'staff' | 'messages'

const bottomNavBarHeight = 60

const Root = styled.div`
  position: fixed;
  left: 0%;
  right: 0%;
  bottom: 0%;
  height: ${bottomNavBarHeight}px;

  display: flex;
  justify-content: space-evenly;
  align-items: center;

  background: ${colors.blues.primary};
  box-shadow: 0px -4px 10px rgba(0, 0, 0, 0.15);
  margin-bottom: 0 !important;
`

export const ReserveSpace = styled.div`
  height: ${bottomNavBarHeight}px;
`

const Button = styled.div`
  width: 100px;
  position: relative;
`

const CustomIcon = styled(FontAwesomeIcon)<{ selected: boolean }>`
  color: ${(p) => (p.selected ? colors.blues.lighter : colors.blues.light)};
  height: 24px !important;
  width: 24px !important;
  margin: 0;
`

const IconText = styled.span<{ selected: boolean }>`
  color: ${(p) => (p.selected ? colors.blues.lighter : colors.blues.light)};
  font-size: 14px;
`

const Circle = styled.span<{ color: keyof typeof colors.accents }>`
  min-width: 16px;
  background-color: ${(p) => colors.accents[p.color]};
  color: ${colors.greyscale.white};
  border-radius: 8px;
  display: inline-block;
  position: absolute;
  right: 0;
  top: -4px;
  padding: 0 4.5px;
  font-size: 11px;
`

type BottomTextProps = {
  text: string
  children: React.ReactNode
  selected: boolean
  onClick: () => void
}

const BottomText = ({ text, children, selected, onClick }: BottomTextProps) => {
  return (
    <FixedSpaceColumn spacing="3px" onClick={onClick}>
      <FixedSpaceRow justifyContent="center" marginBottom="zero">
        {children}
      </FixedSpaceRow>
      <FixedSpaceRow justifyContent="space-evenly" alignItems="center">
        <IconText selected={selected}>{text}</IconText>
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
}

type BottomNavbarProps = {
  selected?: NavItem
  messageCount?: number
  staffCount?: { count: number; countOther: number }
  onChange?: (value: NavItem) => void
}

export default function BottomNavbar({
  selected,
  messageCount,
  staffCount,
  onChange
}: BottomNavbarProps) {
  const { i18n } = useTranslation()

  return (
    <>
      {/* Reserve navbar's height from the page, so that the fixed navbar doesn't hide anything */}
      <ReserveSpace />
      <Root>
        <Button data-qa="bottomnav-children">
          <BottomText
            text={i18n.common.children}
            selected={selected === 'child'}
            onClick={() =>
              selected !== 'child' && onChange && onChange('child')
            }
          >
            <CustomIcon icon={faChild} selected={selected === 'child'} />
          </BottomText>
        </Button>
        <Button data-qa="bottomnav-staff">
          <BottomText
            text={i18n.common.staff}
            selected={selected === 'staff'}
            onClick={() =>
              selected !== 'staff' && onChange && onChange('staff')
            }
          >
            <CustomIcon icon={faUser} selected={selected === 'staff'} />
            {staffCount &&
            (staffCount.count != 0 || staffCount.countOther != 0) ? (
              <Circle color="violet" data-qa="staff-count-bubble">
                {formatDecimal(staffCount.count)}+
                {formatDecimal(staffCount.countOther)}
              </Circle>
            ) : null}
          </BottomText>
        </Button>
        <div style={{ display: 'none' }}>
          {/* This will be needed in the future */}
          <Button data-qa="messages">
            <BottomText
              text={i18n.common.messages}
              selected={selected === 'messages'}
              onClick={() =>
                selected !== 'messages' && onChange && onChange('messages')
              }
            >
              <CustomIcon
                icon={faComments}
                selected={selected === 'messages'}
              />
              {messageCount || 0 > 0 ? (
                <Circle color="orange">{messageCount}</Circle>
              ) : null}
            </BottomText>
          </Button>
        </div>
      </Root>
    </>
  )
}
