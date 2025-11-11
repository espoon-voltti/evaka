// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled, { css } from 'styled-components'
import { Link } from 'wouter'

import type { IconSize } from 'lib-components/atoms/icon-size'
import { diameterByIconSize } from 'lib-components/atoms/icon-size'
import type { BaseProps } from 'lib-components/utils'
import { defaultMargins } from 'lib-components/white-space'

import { buttonBorderRadius, defaultButtonTextStyle } from './button-commons'

type BaseLinkProps = {
  icon?: IconDefinition
  text?: string
  size?: IconSize
} & (
  | { 'aria-label': string; 'aria-labelledby'?: never }
  | { 'aria-label'?: never; 'aria-labelledby': string }
  | { 'aria-label'?: never; 'aria-labelledby'?: never }
) &
  BaseProps

export type InlineButtonLinkProps = BaseLinkProps & {
  to: string
}

export type InlineButtonHrefProps = BaseLinkProps & {
  href: string
  newTab?: boolean
}

// Shared styles matching the appearance="inline" button from button-visuals.tsx
const inlineButtonStyles = css<{ $hasText: boolean; $size: IconSize }>`
  ${(p) => (p.$hasText ? defaultButtonTextStyle : '')};
  color: ${(p) => p.theme.colors.main.m2};
  border: none;
  border-radius: ${buttonBorderRadius};
  background: none;
  padding: 0;
  display: inline-flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: center;
  outline: none;
  cursor: pointer;
  text-decoration: none;
  ${(p) =>
    !p.$hasText &&
    css`
      width: ${diameterByIconSize(p.$size)}px;
      height: ${diameterByIconSize(p.$size)}px;
    `};

  &:focus {
    outline: 2px solid ${(p) => p.theme.colors.main.m2Focus};
    outline-offset: 2px;
  }

  &:hover {
    color: ${(p) => p.theme.colors.main.m1};
  }

  &:active {
    color: ${(p) => p.theme.colors.main.m2Active};
  }

  svg {
    ${(p) =>
      p.$hasText &&
      css`
        margin-right: ${defaultMargins.xs};
        font-size: 1.25em;
      `}
  }
`

const StyledLink = styled(Link)<{ $hasText: boolean; $size: IconSize }>`
  ${inlineButtonStyles}
`

const StyledAnchor = styled.a<{ $hasText: boolean; $size: IconSize }>`
  ${inlineButtonStyles}
`

export const InlineInternalLinkButton = React.memo(
  function InlineInternalLinkButton({
    icon,
    text,
    size = 's',
    className,
    to,
    'data-qa': dataQa,
    'aria-label': ariaLabel,
    'aria-labelledby': ariaLabelledby
  }: InlineButtonLinkProps) {
    const content = (
      <>
        {icon ? <FontAwesomeIcon icon={icon} /> : null}
        {text ? <span>{text}</span> : null}
      </>
    )

    return (
      <StyledLink
        to={to}
        $hasText={!!text}
        $size={size}
        className={className}
        data-qa={dataQa}
        aria-label={ariaLabel}
        aria-labelledby={ariaLabelledby}
      >
        {content}
      </StyledLink>
    )
  }
)

export const InlineExternalLinkButton = React.memo(
  function InlineExternalLinkButton({
    icon,
    text,
    size = 's',
    className,
    href,
    newTab,
    'data-qa': dataQa,
    'aria-label': ariaLabel,
    'aria-labelledby': ariaLabelledby
  }: InlineButtonHrefProps) {
    const content = (
      <>
        {icon ? <FontAwesomeIcon icon={icon} /> : null}
        {text ? <span>{text}</span> : null}
      </>
    )

    const targetProps = newTab
      ? { target: '_blank' as const, rel: 'noopener noreferrer' }
      : {}

    return (
      <StyledAnchor
        href={href}
        $hasText={!!text}
        $size={size}
        className={className}
        data-qa={dataQa}
        aria-label={ariaLabel}
        aria-labelledby={ariaLabelledby}
        {...targetProps}
      >
        {content}
      </StyledAnchor>
    )
  }
)
