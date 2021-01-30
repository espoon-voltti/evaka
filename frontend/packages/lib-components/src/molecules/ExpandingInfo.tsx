// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faInfo, faTimes } from '@evaka/lib-icons'
import React, { useState } from 'react'
import styled from 'styled-components'
import colors, { blueColors } from '../colors'
import { defaultMargins, Gap } from '../white-space'
import RoundIcon from '../atoms/RoundIcon'
import { P } from '@evaka/lib-components/src/typography'
import Container from '@evaka/lib-components/src/layout/Container'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import {
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'

const TooltipPositioner = styled.div`
  width: 100%;
  justify-content: center;
  align-items: center;
`

const TooltipContainer = styled.div`
  color: ${blueColors.dark};
  font-size: 15px;
  line-height: 22px;

  background-color: rgba(36, 159, 255, 0.1);
  padding: ${defaultMargins.s};

  p:not(:last-child) {
    margin-bottom: 8px;
  }
`

const FlexDiv = styled.div`
  display: flex;
`

const CloseButton = styled.div`
  text-align: right;
`

const RightMargin = styled.div`
  margin-right: 15px;
`

type ExpandingInfoProps = {
  children: React.ReactNode
  info: JSX.Element
}

export default function ExpandingInfo({ children, info }: ExpandingInfoProps) {
  const [expanded, setExpanded] = useState<boolean>(false)

  return (
    <>
      <FixedSpaceRow spacing="xs">
        <div>{children}</div>
        <RoundIcon
          content={faInfo}
          color={colors.brandEspoo.espooTurquoise}
          size="s"
          onClick={() => {
            setExpanded(true)
          }}
        />
      </FixedSpaceRow>
      {expanded && (
        <div style={{ marginLeft: '-32px', marginRight: '-32px' }}>
          <TooltipPositioner className={'tooltip'}>
            <TooltipContainer>
              <Container>
                <FixedSpaceFlexWrap
                  style={{ justifyContent: 'space-between' }}
                  reverse={true}
                >
                  <FlexDiv>
                    <Gap horizontal size={'m'} />
                    <RightMargin>
                      <RoundIcon
                        content={faInfo}
                        color={colors.brandEspoo.espooTurquoise}
                        size="s"
                      />
                    </RightMargin>
                    <P style={{ margin: '0' }}>{info}</P>
                  </FlexDiv>
                  <span />
                  <CloseButton>
                    <InlineButton
                      onClick={() => {
                        setExpanded(false)
                      }}
                      icon={faTimes}
                      text={''}
                    />
                  </CloseButton>
                </FixedSpaceFlexWrap>
              </Container>
            </TooltipContainer>
          </TooltipPositioner>
        </div>
      )}
    </>
  )
}
