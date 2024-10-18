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
  h2: '20px',
  h3: '18px',
  h4: '16px'
}

export type HeadingProps = BaseProps & {
  children: React.ReactNode
  fitted?: boolean
  'data-qa'?: string
  centered?: boolean
  noMargin?: boolean
  smaller?: boolean
  primary?: boolean
}

export const H1 = styled.h1<HeadingProps>`
  color: ${(p) =>
    p.primary || p.primary === undefined
      ? p.theme.colors.main.m1
      : p.theme.colors.grayscale.g100};
  font-size: ${(p) => (p.smaller ? fontSizesMobile.h1 : '32px')};
  line-height: ${(p) => (p.smaller ? fontSizesMobile.h1 : 'initial')};
  font-family: Montserrat, sans-serif;
  font-weight: ${(p) => p.theme.typography.h1.weight};
  ${(p) =>
    !p.fitted
      ? css`
          margin-bottom: ${defaultMargins.m};
        `
      : ''}
  ${(p) =>
    p.centered
      ? css`
          text-align: center;
        `
      : ''}
  ${(p) =>
    p.noMargin
      ? css`
          margin: 0;
        `
      : ''}

  @media (max-width: 600px) {
    font-size: ${fontSizesMobile.h1};
    ${(p) =>
      p.theme.typography.h1.mobile?.weight &&
      css`
        font-weight: ${p.theme.typography.h1.mobile?.weight};
      `}
  }
`

export const H2 = styled.h2<HeadingProps>`
  color: ${(p) =>
    p.primary ? p.theme.colors.main.m1 : p.theme.colors.grayscale.g100};
  font-size: ${(p) => (p.smaller ? fontSizesMobile.h2 : '24px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${(p) => p.theme.typography.h2.weight};
  ${(p) =>
    !p.fitted
      ? css`
          margin-bottom: ${defaultMargins.s};
        `
      : ''}
  ${(p) =>
    p.centered
      ? css`
          text-align: center;
        `
      : ''}
  ${(p) =>
    p.noMargin
      ? css`
          margin: 0;
        `
      : ''}

  @media (max-width: 600px) {
    color: ${(p) => p.theme.colors.main.m1};
    font-size: ${fontSizesMobile.h2};
    ${(p) =>
      p.theme.typography.h2.mobile?.weight &&
      css`
        font-weight: ${p.theme.typography.h2.mobile?.weight};
      `}
  }
`

export const H3 = styled.h3<HeadingProps>`
  color: ${(p) =>
    p.primary ? p.theme.colors.main.m1 : p.theme.colors.grayscale.g100};
  font-size: ${(p) => (p.smaller ? fontSizesMobile.h3 : '20px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${(p) => p.theme.typography.h3.weight};
  ${(p) =>
    !p.fitted
      ? css`
          margin-bottom: ${defaultMargins.s};
        `
      : ''}
  ${(p) =>
    p.centered
      ? css`
          text-align: center;
        `
      : ''}
  ${(p) =>
    p.noMargin
      ? css`
          margin: 0;
        `
      : ''}

  @media (max-width: 600px) {
    font-size: ${fontSizesMobile.h3};
    ${(p) =>
      p.theme.typography.h3.mobile?.weight &&
      css`
        font-weight: ${p.theme.typography.h3.mobile?.weight};
      `}
  }
`

export const H4 = styled.h4<HeadingProps>`
  color: ${(p) =>
    p.primary ? p.theme.colors.main.m1 : p.theme.colors.grayscale.g100};
  font-size: ${(p) => (p.smaller ? fontSizesMobile.h4 : '18px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${(p) => p.theme.typography.h4.weight};
  ${(p) =>
    !p.fitted
      ? css`
          margin-bottom: ${defaultMargins.s};
        `
      : ''}
  ${(p) =>
    p.centered
      ? css`
          text-align: center;
        `
      : ''}
  ${(p) =>
    p.noMargin
      ? css`
          margin: 0;
        `
      : ''}

  @media (max-width: 600px) {
    font-size: ${fontSizesMobile.h4};
    ${(p) =>
      p.theme.typography.h4.mobile?.weight &&
      css`
        font-weight: ${p.theme.typography.h4.mobile?.weight};
      `}
  }
`

export interface LabelProps {
  inputRow?: boolean
  primary?: boolean
  white?: boolean
}

const labelMixin = css<LabelProps>`
  font-weight: ${fontWeights.semibold};
  color: ${(p) =>
    p.primary
      ? p.theme.colors.main.m1
      : p.white
        ? p.theme.colors.grayscale.g0
        : p.theme.colors.grayscale.g100};
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
  ${(p) => (p.centered ? `text-align: center;` : '')};
  max-width: ${(p) => p.width || '960px'};
  ${(p) =>
    p.noMargin ? `margin: 0` : `margin-block: ${p.fitted ? `0` : '1.5em'}`};
  ${(p) => (p.preserveWhiteSpace ? 'white-space: pre-line' : '')};
  ${(p) => (p.color ? `color: ${p.color};` : '')};

  strong {
    color: ${(p) => p.theme.colors.grayscale.g100};
    font-weight: ${fontWeights.semibold};
  }

  a {
    color: ${(p) => p.theme.colors.main.m2};
    text-decoration: underline;
  }

  a:hover {
    color: ${(p) => p.theme.colors.main.m2Hover};
    text-decoration: none;
  }

  a:focus {
    outline: 1px solid ${(p) => p.theme.colors.main.m2Focus};
    text-decoration: none;
  }

  a:visited {
    color: ${(p) => p.theme.colors.accents.a4violet};
  }
`

export const Dimmed = styled.span`
  color: ${(p) => p.theme.colors.grayscale.g70};
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
  color: ${(p) => p.theme.colors.grayscale.g70};
`

export const Title = styled.span<{ primary?: boolean; centered?: boolean }>`
  font-family: Montserrat, sans-serif;
  font-size: 20px;
  font-weight: ${fontWeights.semibold};
  color: ${(p) =>
    p.primary ? p.theme.colors.main.m1 : p.theme.colors.grayscale.g100};
  ${(p) =>
    p.centered &&
    css`
      text-align: center;
    `}
`

export const BigNumber = styled.span<{ centered?: boolean }>`
  font-family: Montserrat, sans-serif;
  font-size: 60px;
  font-weight: ${fontWeights.light};
  line-height: 73px;
  color: ${(p) => p.theme.colors.main.m1};
  ${(p) =>
    p.centered &&
    css`
      text-align: center;
    `}
`

export const InformationText = styled.span<{ centered?: boolean }>`
  color: ${(p) => p.theme.colors.grayscale.g70};
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
  color: ${(p) => p.theme.colors.grayscale.g100};
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
