import colors from '@evaka/lib-components/colors'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/layout/flex-helpers'
import { Gap } from '@evaka/lib-components/white-space'
import { faChild, faComments, faUser } from '@evaka/lib-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'

type SelectedNavButton = 'child' | 'staff' | 'messages'

const FooterDiv = styled.div`
  position: fixed;
  left: 0%;
  right: 0%;
  bottom: 0%;
  height: 60px;

  background: ${colors.blues.primary};
  box-shadow: 0px -4px 10px rgba(0, 0, 0, 0.15);
  margin-bottom: 0 !important;
`

const LeftButton = styled.div`
  position: fixed;
  left: 10%;
`

const MiddleButton = styled.div`
  position: fixed;
  left: 50%;
  transform: translateX(-50%);
`

const RightButton = styled.div`
  position: fixed;
  right: 10%;
`

const CustomIcon = styled(FontAwesomeIcon)<{ selected: boolean }>`
  color: ${(p) => (p.selected ? colors.blues.lighter : colors.blues.light)};
  height: 24px !important;
  width: 24px !important;
  margin 0;
`

const IconText = styled.span<{ selected: boolean }>`
  color: ${(p) => (p.selected ? colors.blues.lighter : colors.blues.light)};
  fontsize: 14px;
`

const Circle = styled.span`
  height: 16px;
  width: 16px;
  background-color: ${colors.accents.orange};
  color: ${colors.greyscale.white};
  border-radius: 50%;
  display: inline-block;
  position: absolute;
  right: 8px;
  top: -4px;
  padding-left: 4.5px;
  font-size: 11px;
`

type IconWithBottomTextProps = {
  text: string
  children: React.ReactNode
  selected: boolean
}

const BottomText = ({ text, children, selected }: IconWithBottomTextProps) => {
  return (
    <FixedSpaceColumn>
      <FixedSpaceRow style={{ margin: '0 auto' }}>{children}</FixedSpaceRow>
      <FixedSpaceRow>
        <IconText selected={selected}>{text}</IconText>
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
}

type BottomNavbarProps = {
  selected?: SelectedNavButton
  messageCount?: number
}

export default function BottomNavbar({
  selected,
  messageCount
}: BottomNavbarProps) {
  const { i18n } = useTranslation()

  return (
    <FooterDiv>
      <Gap size={'xs'} />
      <LeftButton>
        <BottomText text={i18n.common.children} selected={selected === 'child'}>
          <CustomIcon
            icon={faChild}
            onClick={() => null}
            selected={selected === 'child'}
          />
        </BottomText>
      </LeftButton>
      <MiddleButton>
        <BottomText text={i18n.common.staff} selected={selected === 'staff'}>
          <CustomIcon
            icon={faUser}
            onClick={() => null}
            selected={selected === 'staff'}
          />
        </BottomText>
      </MiddleButton>
      <RightButton>
        <BottomText
          text={i18n.common.messages}
          selected={selected === 'messages'}
        >
          <CustomIcon
            icon={faComments}
            onClick={() => null}
            selected={selected === 'messages'}
          />
          {messageCount || 0 > 0 ? <Circle>{messageCount}</Circle> : null}
        </BottomText>
      </RightButton>
    </FooterDiv>
  )
}
