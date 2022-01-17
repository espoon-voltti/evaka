// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled, { css } from 'styled-components'
import { defaultMargins } from 'lib-components/white-space'
import { tabletMin } from './breakpoints'

import { BaseProps } from './utils'

export const fontWeights = {
  light: 300,
  normal: 400,
  medium: 500,
  semibold: 600,
  bold: 700
}

export const fontSizesMobile = {
  h1: '24px',
  h2: '22px',
  h3: '18px',
  h4: '16px'
}

type HeadingProps = BaseProps & {
  fitted?: boolean
  'data-qa'?: string
  centered?: boolean
  noMargin?: boolean
  smaller?: boolean
  primary?: boolean
}

export const H1 = styled.h1<HeadingProps>`
  color: ${({ theme: { colors }, ...p }) =>
    p.primary || p.primary === undefined
      ? colors.main.dark
      : colors.greyscale.darkest};
  font-size: ${(p) => (p.smaller ? fontSizesMobile.h1 : '36px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${({ theme: { typography } }) => typography.h1.weight};
  ${(p) => (!p.fitted ? `margin-bottom: ${defaultMargins.m};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}

  @media (max-width: 600px) {
    font-size: ${fontSizesMobile.h1};
    ${({ theme: { typography } }) =>
      typography.h1.mobile?.weight &&
      `font-weight: ${typography.h1.mobile?.weight};`}
  }
`

export const H2 = styled.h2<HeadingProps>`
  color: ${({ theme: { colors }, ...p }) =>
    p.primary ? colors.main.dark : colors.greyscale.darkest};
  font-size: ${(p) => (p.smaller ? fontSizesMobile.h2 : '24px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${({ theme: { typography } }) => typography.h2.weight};
  ${(p) => (!p.fitted ? `margin-bottom: ${defaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}

  @media (max-width: 600px) {
    font-size: ${fontSizesMobile.h2};
    ${({ theme: { typography } }) =>
      typography.h2.mobile?.weight &&
      `font-weight: ${typography.h2.mobile?.weight};`}
  }
`

export const H3 = styled.h3<HeadingProps>`
  color: ${({ theme: { colors }, ...p }) =>
    p.primary ? colors.main.dark : colors.greyscale.darkest};
  font-size: ${(p) => (p.smaller ? fontSizesMobile.h3 : '20px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${({ theme: { typography } }) => typography.h3.weight};
  ${(p) => (!p.fitted ? `margin-bottom: ${defaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}

  @media (max-width: 600px) {
    font-size: ${fontSizesMobile.h3};
    ${({ theme: { typography } }) =>
      typography.h3.mobile?.weight &&
      `font-weight: ${typography.h3.mobile?.weight};`}
  }
`

export const H4 = styled.h4<HeadingProps>`
  color: ${({ theme: { colors }, ...p }) =>
    p.primary ? colors.main.dark : colors.greyscale.darkest};
  font-size: ${(p) => (p.smaller ? fontSizesMobile.h4 : '18px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${({ theme: { typography } }) => typography.h4.weight};
  ${(p) => (!p.fitted ? `margin-bottom: ${defaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}

  @media (max-width: 600px) {
    font-size: ${fontSizesMobile.h4};
    ${({ theme: { typography } }) =>
      typography.h4.mobile?.weight &&
      `font-weight: ${typography.h4.mobile?.weight};`}
  }
`

interface LabelProps {
  inputRow?: boolean
  primary?: boolean
  white?: boolean
}

const labelMixin = css<LabelProps>`
  font-weight: ${fontWeights.semibold};
  color: ${(p) =>
    p.primary
      ? p.theme.colors.main.dark
      : p.white
      ? p.theme.colors.greyscale.white
      : p.theme.colors.greyscale.darkest};
  ${(p) => (p.inputRow ? 'margin-top: 6px;' : '')}
`

export const Label = styled.label<LabelProps>`
  ${labelMixin}
`

export const LabelLike = styled.div<LabelProps>`
  ${labelMixin}
`

type ParagraphProps = {
  fitted?: boolean
  noMargin?: boolean
  centered?: boolean
  width?: string
  preserveWhiteSpace?: boolean
  color?: string
}

export const P = styled.p<ParagraphProps>`
  ${(p) => (p.centered ? 'text-align: center;' : '')};
  max-width: ${(p) => p.width || '960px'};
  ${(p) =>
    p.noMargin ? `margin: 0` : `margin-block: ${p.fitted ? `0` : '1.5em'}`};
  ${(p) => (p.preserveWhiteSpace ? 'white-space: pre-line' : '')};
  ${(p) => (p.color ? `color: ${p.color};` : '')};

  strong {
    color: ${(p) => p.theme.colors.greyscale.darkest};
    font-weight: ${fontWeights.semibold};
  }

  a {
    color: ${(p) => p.theme.colors.main.primary};
  }

  a:hover {
    color: ${(p) => p.theme.colors.main.dark};
    cursor: pointer;
    text-decoration: underline;
  }

  a:focus {
    outline: 1px solid ${(p) => p.theme.colors.main.light};
  }

  a:visited {
    color: ${(p) => p.theme.colors.accents.violet};
  }
`

export const Dimmed = styled.span`
  color: ${({ theme: { colors } }) => colors.greyscale.dark};
`

export const Strong = styled.span`
  font-weight: ${fontWeights.bold};
`

export const Bold = styled.span`
  font-weight: ${fontWeights.semibold};
`

export const Italic = styled.span`
  font-style: italic;
`

export const Light = styled.span`
  font-style: italic;
  color: ${({ theme: { colors } }) => colors.greyscale.dark};
`

export const Title = styled.span<{ primary?: boolean }>`
  font-family: Montserrat, sans-serif;
  color: ${(p) =>
    p.primary ? p.theme.colors.main.primary : p.theme.colors.greyscale.darkest};
  font-size: 20px;
  font-weight: ${fontWeights.semibold};
`

export const BigNumber = styled.span`
  font-family: Montserrat, sans-serif;
  font-size: 60px;
  font-weight: ${fontWeights.light};
  line-height: 73px;
  color: ${(p) => p.theme.colors.main.dark};
`

export const InformationText = styled.span<{ centered?: boolean }>`
  color: ${(p) => p.theme.colors.greyscale.dark};
  font-size: 14px;
  font-weight: ${fontWeights.semibold};
  ${(p) =>
    p.centered
      ? css`
          text-align: center;
        `
      : ''}
`

export const NavLinkText = styled.span`
  color: ${(p) => p.theme.colors.greyscale.darkest};
  cursor: pointer;
  font-family: Montserrat, sans-serif;
  font-weight: ${fontWeights.medium};
  font-size: 14px;
  line-height: 16px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  text-decoration: none;

  @media (min-width: ${tabletMin}) {
    font-size: 15px;
  }
`
