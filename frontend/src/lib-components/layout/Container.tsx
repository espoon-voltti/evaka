// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { ReactNode } from 'react'
import styled, { useTheme } from 'styled-components'

import { faChevronDown, faChevronUp } from 'lib-icons'

import RoundIcon from '../atoms/RoundIcon'
import { desktopMin } from '../breakpoints'
import { defaultMargins, Gap, isSpacingSize, SpacingSize } from '../white-space'

export const Container = styled.div<{ verticalMargin?: string }>`
  margin: ${({ verticalMargin }) => (verticalMargin ? verticalMargin : '0')}
    auto;
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

const spacing = (
  // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
  spacing?: SpacingSize | string,
  defaultValue = defaultMargins.s
) =>
  spacing
    ? isSpacingSize(spacing)
      ? defaultMargins[spacing]
      : spacing
    : defaultValue

type ContentAreaProps = {
  classname?: string
  'data-qa'?: string
  opaque: boolean
  fullHeight?: boolean
  // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
  paddingVertical?: SpacingSize | string
  // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
  paddingHorizontal?: SpacingSize | string
  blue?: boolean
  shadow?: boolean
}

export const ContentArea = styled.section<ContentAreaProps>`
  padding: ${(p) =>
    `${spacing(p.paddingVertical, defaultMargins.m)} ${spacing(
      p.paddingHorizontal
    )}`};

  // wider default horizontal paddings on desktop
  @media screen and (min-width: ${desktopMin}) {
    padding: ${(p) =>
      `${spacing(p.paddingVertical, defaultMargins.L)} ${spacing(
        p.paddingHorizontal,
        defaultMargins.L
      )}`};
  }

  background-color: ${(p) =>
    p.opaque ? 'white' : p.blue ? p.theme.colors.main.m4 : 'transparent'};
  position: relative;
  ${(p) => (p.fullHeight ? `min-height: 100vh` : '')}
  ${(p) =>
    p.shadow
      ? `box-shadow: 0px 4px 4px 0px ${p.theme.colors.grayscale.g15}`
      : ''}
`

export type CollapsibleContentAreaProps = ContentAreaProps & {
  open: boolean
  toggleOpen: () => void
  title: ReactNode
  children: ReactNode
  countIndicator?:
    | number
    | {
        count: number
        ariaLabel: string
      }
  icon?: IconDefinition
}

const IconContainer = styled.div`
  display: flex;
`

export const CollapsibleContentArea = React.memo(
  function CollapsibleContentArea({
    open,
    toggleOpen,
    title,
    children,
    countIndicator = 0,
    icon,
    ...props
  }: CollapsibleContentAreaProps) {
    const { colors } = useTheme()

    const showCountIndicator =
      typeof countIndicator === 'number'
        ? countIndicator > 0
        : countIndicator.count > 0

    return (
      <ContentArea {...props} data-status={open ? 'open' : 'closed'}>
        <TitleContainer
          onClick={toggleOpen}
          data-qa={props['data-qa'] ? `${props['data-qa']}-header` : undefined}
          className={classNames({ open })}
          role="button"
          aria-expanded={open}
        >
          {icon && <FontAwesomeIcon icon={icon} />}
          {title}
          <IconContainer>
            {showCountIndicator && (
              <>
                <RoundIcon
                  size="m"
                  color={colors.status.warning}
                  content={(typeof countIndicator === 'number'
                    ? countIndicator
                    : countIndicator.count
                  ).toString()}
                  aria-label={
                    typeof countIndicator === 'number'
                      ? undefined
                      : countIndicator.ariaLabel
                  }
                  data-qa="count-indicator"
                />
                <Gap horizontal={true} size="s" />
              </>
            )}
            <TitleIcon
              icon={open ? faChevronUp : faChevronDown}
              data-qa="collapsible-trigger"
            />
          </IconContainer>
        </TitleContainer>
        <Collapsible open={open}>{open && children}</Collapsible>
      </ContentArea>
    )
  }
)

export const TitleContainer = styled.button`
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: space-between;
  align-items: center;
  overflow: visible;
  font: inherit;
  font-size: inherit;
  line-height: normal;
  color: inherit;
  background: transparent;
  outline: none;
  border: 2px solid transparent;
  border-radius: 2px;
  text-align: left;
  gap: ${defaultMargins.xs};
  margin: -${defaultMargins.xs};
  padding: ${defaultMargins.xs};
  width: calc(100% + 2 * ${defaultMargins.xs});

  :focus {
    border-color: ${(p) => p.theme.colors.main.m2Focus};
  }

  & > * {
    margin: 0;
  }
`

export const TitleIcon = styled(FontAwesomeIcon)`
  cursor: pointer;
  color: ${(p) => p.theme.colors.main.m2};
  height: 24px !important;
  width: 24px !important;
`
const Collapsible = styled.div<{ open: boolean }>`
  display: ${(props) => (props.open ? 'block' : 'none')};
  margin-top: ${defaultMargins.s};
`

export default Container
