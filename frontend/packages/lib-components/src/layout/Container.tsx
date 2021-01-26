// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { KeyboardEvent, ReactNode } from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faChevronDown, faChevronUp } from '@evaka/lib-icons'
import colors from '../colors'
import { defaultMargins, SpacingSize } from '../white-space'
import { BaseProps } from '../utils'

export const Container = styled.div`
  margin: 0 auto;
  position: relative;

  @media screen and (min-width: 1024px) {
    max-width: 960px;
    width: 960px;
  }
  @media screen and (max-width: 1215px) {
    max-width: 1152px;
    width: auto;
  }
  @media screen and (max-width: 1407px) {
    max-width: 1344px;
    width: auto;
  }
  @media screen and (min-width: 1216px) {
    max-width: 1152px;
    width: 1152px;
  }
  @media screen and (min-width: 1408px) {
    max-width: 1344px;
    width: 1344px;
  }
`

type ContentAreaProps = BaseProps & {
  opaque: boolean
  paddingVertical?: SpacingSize
  paddingHorozontal?: SpacingSize
}

export const ContentArea = styled.section<ContentAreaProps>`
  padding: ${(p) =>
    `${
      p.paddingVertical ? defaultMargins[p.paddingVertical] : defaultMargins.s
    } ${
      p.paddingHorozontal
        ? defaultMargins[p.paddingHorozontal]
        : defaultMargins.L
    }`};
  background-color: ${(props) => (props.opaque ? 'white' : 'transparent')};
  position: relative;
`

type CollapsibleContentAreaProps = ContentAreaProps & {
  open: boolean
  toggleOpen: () => void
  title: ReactNode
  children: ReactNode
}

export const CollapsibleContentArea = React.memo(
  function CollapsibleContentArea({
    open,
    toggleOpen,
    title,
    children,
    ...props
  }: CollapsibleContentAreaProps) {
    const toggleOnEnter = (event: KeyboardEvent<HTMLDivElement>) => {
      if (event.key === 'Enter') {
        toggleOpen()
      }
    }

    return (
      <ContentArea {...props}>
        <TitleContainer
          tabIndex={0}
          onClick={toggleOpen}
          onKeyUp={toggleOnEnter}
        >
          <TitleWrapper>{title}</TitleWrapper>
          <TitleIcon icon={open ? faChevronUp : faChevronDown} />
        </TitleContainer>
        {open ? children : null}
      </ContentArea>
    )
  }
)

const TitleContainer = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
`

const TitleWrapper = styled.div`
  flex-grow: 1;
`

const TitleIcon = styled(FontAwesomeIcon)`
  color: ${colors.blues.primary};
  height: 24px !important;
  width: 24px !important;
`

export default Container
