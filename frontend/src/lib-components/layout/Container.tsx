// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import type { ReactNode } from 'react'
import React from 'react'
import styled, { css, useTheme } from 'styled-components'

import { faChevronDown, faChevronUp } from 'lib-icons'

import RoundIcon from '../atoms/RoundIcon'
import { desktopMin } from '../breakpoints'
import type { SpacingSize } from '../white-space'
import { defaultMargins, Gap, isSpacingSize } from '../white-space'

export const Container = styled.div<{
  verticalMargin?: string
  wide?: boolean
}>`
  margin: ${({ verticalMargin }) => (verticalMargin ? verticalMargin : '0')}
    auto;
  position: relative;

  ${({ wide }) =>
    wide
      ? css`
          /* When wide=true: use all available width with responsive margins, max 1600px, centered */
          /* 16px margins for screens up to 1200px */
          width: calc(100% - 32px);
          max-width: 1568px; /* 1600px - 32px for margins */
          margin-left: auto;
          margin-right: auto;

          /* 32px margins for screens wider than 1200px */
          @media screen and (min-width: 1200px) {
            width: calc(100% - 64px);
            max-width: 1536px; /* 1600px - 64px for margins */
          }
        `
      : css`
          /* When wide=false: original behavior */
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
        `}
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
  alwaysShownContent?: ReactNode
  children: ReactNode
  countIndicator?:
    | number
    | {
        count: number
        ariaLabel: string
      }
  countIndicatorColor?: string
  icon?: IconDefinition
  slim?: boolean
}

const IconContainer = styled.div`
  display: flex;
`

export const CollapsibleContentArea = React.memo(
  function CollapsibleContentArea({
    open,
    toggleOpen,
    title,
    alwaysShownContent,
    children,
    countIndicator = 0,
    countIndicatorColor,
    icon,
    slim,
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
                  color={countIndicatorColor ?? colors.status.warning}
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
              $small={slim}
            />
          </IconContainer>
        </TitleContainer>
        {alwaysShownContent}
        <Collapsible open={open} $slim={slim}>
          {open && children}
        </Collapsible>
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

  &:focus {
    border-color: ${(p) => p.theme.colors.main.m2Focus};
  }

  & > * {
    margin: 0;
  }
`

export const TitleIcon = styled(FontAwesomeIcon)<{ $small?: boolean }>`
  cursor: pointer;
  color: ${(p) => p.theme.colors.main.m2};
  height: ${(p) => (p.$small ? '20px' : '24px')} !important;
  width: ${(p) => (p.$small ? '20px' : '24px')} !important;
  font-size: ${(p) => (p.$small ? '16px' : '20px')};
`

const Collapsible = styled.div<{ open: boolean; $slim?: boolean }>`
  display: ${(props) => (props.open ? 'block' : 'none')};
  margin-top: ${(p) => (p.$slim ? defaultMargins.xxs : defaultMargins.s)};
`

export default Container
