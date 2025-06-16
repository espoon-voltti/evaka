// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { Link } from 'wouter'

import { fontWeights } from '../typography'

type HTMLLinkAttributes = Omit<
  React.AnchorHTMLAttributes<HTMLAnchorElement>,
  'href'
>

export interface Props extends HTMLLinkAttributes {
  className?: string
  to: string
  children: React.ReactNode
  disabled?: boolean
  'data-qa'?: string
}

const StyledLink = styled(Link)`
  font-weight: ${fontWeights.semibold};
  svg {
    font-size: 1.25em;
  }
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;

  &:hover {
    color: ${(p) => p.theme.colors.main.m2Hover};
  }
  &:active {
    color: ${(p) => p.theme.colors.main.m2Active};
  }
`

const StyledSpan = styled.span`
  color: ${(p) => p.theme.colors.grayscale.g70};
  font-weight: ${fontWeights.semibold};
  svg {
    font-size: 1.25em;
  }
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  cursor: not-allowed;

  &:hover {
    color: ${(p) => p.theme.colors.grayscale.g70};
  }
  &:active {
    color: ${(p) => p.theme.colors.grayscale.g70};
  }
`

export default function DisableableLink({
  className,
  children,
  to,
  disabled,
  ...props
}: Props): React.ReactNode {
  if (disabled) {
    return (
      <StyledSpan className={className} {...props}>
        {children}
      </StyledSpan>
    )
  } else {
    return (
      <StyledLink to={to} className={className} {...props}>
        {children}
      </StyledLink>
    )
  }
}
