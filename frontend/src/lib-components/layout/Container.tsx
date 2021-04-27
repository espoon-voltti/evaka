// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { KeyboardEvent, ReactNode } from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faChevronDown, faChevronUp } from 'lib-icons'
import colors from '../colors'
import { defaultMargins, isSpacingSize, SpacingSize } from '../white-space'
import classNames from 'classnames'

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

type ContentAreaProps = {
  classname?: string
  'data-qa'?: string
  opaque: boolean
  fullHeight?: boolean
  paddingVertical?: SpacingSize | string
  paddingHorizontal?: SpacingSize | string
  blue?: boolean
  shadow?: boolean
}

export const ContentArea = styled.section<ContentAreaProps>`
  padding: ${(p) =>
    `${
      p.paddingVertical
        ? isSpacingSize(p.paddingVertical)
          ? defaultMargins[p.paddingVertical]
          : p.paddingVertical
        : defaultMargins.s
    } ${
      p.paddingHorizontal
        ? isSpacingSize(p.paddingHorizontal)
          ? defaultMargins[p.paddingHorizontal]
          : p.paddingHorizontal
        : defaultMargins.L
    }`};
  background-color: ${(props) =>
    props.opaque
      ? 'white'
      : props.blue
      ? colors.brandEspoo.espooTurquoiseLight
      : 'transparent'};
  position: relative;
  ${(p) => (p.fullHeight ? `min-height: 100vh` : '')}
  ${(p) =>
    p.shadow ? `box-shadow: 0px 4px 4px 0px ${colors.greyscale.lighter}` : ''}
`

type CollapsibleContentAreaProps = ContentAreaProps & {
  open: boolean
  toggleOpen: () => void
  title: ReactNode
  children: ReactNode
  validationErrors?: number
}

export const CollapsibleContentArea = React.memo(
  function CollapsibleContentArea({
    open,
    toggleOpen,
    title,
    children,
    validationErrors,
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
          data-qa={props['data-qa'] ? `${props['data-qa']}-header` : undefined}
          onKeyUp={toggleOnEnter}
          className={classNames({ open })}
          open={open}
          role="button"
        >
          <TitleWrapper>{title}</TitleWrapper>
          <CollapsibleContentAreaDescription
            numberOfErrors={validationErrors ?? 0}
          />
          <TitleIcon icon={open ? faChevronUp : faChevronDown} />
        </TitleContainer>
        {open ? children : null}
      </ContentArea>
    )
  }
)

function CollapsibleContentAreaDescription({
  numberOfErrors
}: {
  numberOfErrors: number
}) {
  // TODO: add translation
  return (
    <>
      {numberOfErrors > 0 && (
        <StyledP lang="fi">
          {numberOfErrors} kenttää jossa puutteellisia tai virheellisiä tietoja.
        </StyledP>
      )}
    </>
  )
}

const StyledP = styled.p`
  border: 0;
  clip: rect(0 0 0 0);
  height: 1px;
  width: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  position: absolute;
`

const TitleContainer = styled.div<{ open: boolean }>`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;

  cursor: pointer;
  padding: 16px 8px;
  margin: -16px -8px;
  ${(p) => (p.open ? 'margin-bottom: 0' : '')}
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
