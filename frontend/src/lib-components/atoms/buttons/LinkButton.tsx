// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled, { css } from 'styled-components'

import { fontWeights } from 'lib-components/typography'

import { tabletMin } from '../../breakpoints'
import { zoomedMobileMax } from '../../breakpoints'
import { defaultMargins } from '../../white-space'

import { buttonBorderRadius } from './button-commons'

const baseStyles = css`
  -webkit-font-smoothing: antialiased;
  text-size-adjust: 100%;
  box-sizing: inherit;
  height: 45px;
  padding: 0 27px;
  width: fit-content;
  min-width: 100px;
  text-align: center;
  overflow-x: hidden;
  border: 1px solid;
  border-radius: ${buttonBorderRadius};
  outline: none;
  cursor: pointer;
  font-family: 'Open Sans', sans-serif;
  font-size: 1rem;
  line-height: 1rem;
  font-weight: ${fontWeights.semibold};
  white-space: nowrap;
  letter-spacing: 0.2px;
  margin-right: 0;
  text-decoration: none;
  display: flex;
  justify-content: center;
  align-items: center;

  @media (max-width: ${zoomedMobileMax}) {
    white-space: normal;
    line-height: 1.25rem;
    padding: 0 ${defaultMargins.s};
  }

  &:focus {
    box-shadow:
      0 0 0 2px ${(p) => p.theme.colors.grayscale.g0},
      0 0 0 4px ${(p) => p.theme.colors.main.m2Focus};
  }
`

const primaryStyles = css`
  border-color: ${(p) => p.theme.colors.main.m2};
  color: ${(p) => p.theme.colors.grayscale.g0};
  background-color: ${(p) => p.theme.colors.main.m2};

  &:hover {
    background-color: ${(p) => p.theme.colors.main.m2Hover};
  }

  &:active {
    background-color: ${(p) => p.theme.colors.main.m2Active};
  }
`

const secondaryStyles = css`
  border-color: ${(p) => p.theme.colors.main.m2};
  background-color: ${(p) => p.theme.colors.grayscale.g0};

  &:hover {
    color: ${(p) => p.theme.colors.main.m2Hover};
    border-color: ${(p) => p.theme.colors.main.m2Hover};
  }

  &:active {
    color: ${(p) => p.theme.colors.main.m2Active};
    border-color: ${(p) => p.theme.colors.main.m2Active};
  }
`

const LinkButton = styled.a<{ $style?: 'primary' | 'secondary' }>`
  ${baseStyles}
  ${(p) => (p.$style === 'secondary' ? secondaryStyles : primaryStyles)}
`

export const ResponsiveLinkButton = styled(LinkButton)`
  @media (max-width: ${tabletMin}) {
    width: 100%;
  }
`

export default LinkButton
