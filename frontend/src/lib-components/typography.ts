// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { defaultMargins } from 'lib-components/white-space'

import { BaseProps } from './utils'

type HeadingProps = BaseProps & {
  fitted?: boolean
  'data-qa'?: string
  centered?: boolean
  noMargin?: boolean
  smaller?: boolean
  bold?: boolean
  primary?: boolean
}

export const H1 = styled.h1<HeadingProps>`
  color: ${({ theme: { colors }, ...p }) =>
    p.primary || p.primary === undefined
      ? colors.main.dark
      : colors.greyscale.dark};
  font-size: ${(p) => (p.smaller ? '24px' : '36px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${({ theme: { typography }, ...p }) =>
    p.bold ? typography.h1.bold : typography.h1.weight};
  line-height: 58px;
  ${(p) => (!p.fitted ? `margin-bottom: ${defaultMargins.m};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}

  @media (max-width: 600px) {
    font-size: 24px;
    ${({ theme: { typography } }) =>
      typography.h1.mobile?.weight &&
      `font-weight: ${typography.h1.mobile?.weight};`}
    line-height: 36px;
  }
`

export const H2 = styled.h2<HeadingProps>`
  color: ${({ theme: { colors }, ...p }) =>
    p.primary ? colors.main.dark : colors.greyscale.dark};
  font-size: ${(p) => (p.smaller ? '20px' : '24px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${({ theme: { typography }, ...p }) =>
    p.bold ? typography.h2.bold : typography.h2.weight};
  ${(p) => (!p.fitted ? `margin-bottom: ${defaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}

  @media (max-width: 600px) {
    font-size: 20px;
    ${({ theme: { typography } }) =>
      typography.h2.mobile?.weight &&
      `font-weight: ${typography.h2.mobile?.weight};`}
    line-height: 30px;
  }
`

export const H3 = styled.h3<HeadingProps>`
  color: ${({ theme: { colors }, ...p }) =>
    p.primary ? colors.main.dark : colors.greyscale.dark};
  font-size: ${(p) => (p.smaller ? '18px' : '20px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${({ theme: { typography }, ...p }) =>
    p.bold ? typography.h3.bold : typography.h3.weight};
  ${(p) => (!p.fitted ? `margin-bottom: ${defaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}

  @media (max-width: 600px) {
    ${({ theme: { typography } }) =>
      typography.h3.mobile?.weight &&
      `font-weight: ${typography.h3.mobile?.weight};`}
  }
`

export const H4 = styled.h4<HeadingProps>`
  color: ${({ theme: { colors }, ...p }) =>
    p.primary ? colors.main.dark : colors.greyscale.dark};
  font-size: ${(p) => (p.smaller ? '16px' : '18px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${({ theme: { typography }, ...p }) =>
    p.bold ? typography.h4.bold : typography.h4.weight};
  ${(p) => (!p.fitted ? `margin-bottom: ${defaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}

  @media (max-width: 600px) {
    ${({ theme: { typography } }) =>
      typography.h4.mobile?.weight &&
      `font-weight: ${typography.h4.mobile?.weight};`}
  }
`

export const H5 = styled.h4<HeadingProps>`
  color: ${({ theme: { colors }, ...p }) =>
    p.primary ? colors.main.dark : colors.greyscale.dark};
  font-size: ${(p) => (p.smaller ? '14px' : '16px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${({ theme: { typography }, ...p }) =>
    p.bold ? typography.h5.bold : typography.h5.weight};
  ${(p) => (!p.fitted ? `margin-bottom: ${defaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}

  @media (max-width: 600px) {
    ${({ theme: { typography } }) =>
      typography.h5.mobile?.weight &&
      `font-weight: ${typography.h5.mobile?.weight};`}
  }
`

type LabelProps = {
  inputRow?: boolean
}

export const Label = styled.label<LabelProps>`
  font-weight: 600;
  ${(p) => (p.inputRow ? 'margin-top: 6px;' : '')}
`

export const H3LikeLabel = styled.label`
  color: ${({ theme: { colors } }) => colors.greyscale.dark};
  font-size: 20px;
  font-family: Montserrat, sans-serif;
  font-weight: ${({ theme: { typography } }) => typography.h3.weight};

  @media (max-width: 600px) {
    ${({ theme: { typography } }) =>
      typography.h3.mobile?.weight &&
      `font-weight: ${typography.h3.mobile?.weight};`}
  }
`

type ParagraphProps = {
  fitted?: boolean
  centered?: boolean
  width?: string
}

export const P = styled.p<ParagraphProps>`
  ${(p) => (p.centered ? 'text-align: center;' : '')};
  max-width: ${(p) => p.width || '960px'};
  margin-block-start: ${(p) => (p.fitted ? `0` : '1.5em')};
  margin-block-end: ${(p) => (p.fitted ? `0` : '1.5em')};

  strong {
    font-weight: 600;
  }

  a {
    color: ${({ theme: { colors } }) => colors.main.primary};
    text-decoration: underline;
  }

  a:hover {
    text-decoration: underline;
  }
`
export const Dimmed = styled.span`
  color: ${({ theme: { colors } }) => colors.greyscale.dark};
`
