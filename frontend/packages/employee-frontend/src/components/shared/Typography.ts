// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { Greyscale } from 'components/shared/Colors'
import { DefaultMargins } from 'components/shared/layout/white-space'
import { BaseProps } from './utils'

interface HeadingProps extends BaseProps {
  fitted?: boolean
  'data-qa'?: string
  centered?: boolean
  noMargin?: boolean
  smaller?: boolean
  bold?: boolean
}

export const H1 = styled.h1<HeadingProps>`
  color: ${Greyscale.dark};
  font-size: ${(p) => (p.smaller ? '24px' : '36px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${(p) => (p.bold ? 600 : 200)};
  line-height: 58px;
  ${(p) => (!p.fitted ? `margin-bottom: ${DefaultMargins.m};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}
`

export const H2 = styled.h2<HeadingProps>`
  color: ${Greyscale.dark};
  font-size: ${(p) => (p.smaller ? '20px' : '24px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${(p) => (p.bold ? 600 : 300)};
  ${(p) => (!p.fitted ? `margin-bottom: ${DefaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}
`

export const H3 = styled.h3<HeadingProps>`
  color: ${Greyscale.dark};
  font-size: ${(p) => (p.smaller ? '18px' : '20px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${(p) => (p.bold ? 600 : 'normal')};
  ${(p) => (!p.fitted ? `margin-bottom: ${DefaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}
`

export const H4 = styled.h4<HeadingProps>`
  color: ${Greyscale.dark};
  font-size: ${(p) => (p.smaller ? '16px' : '18px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${(p) => (p.bold ? 600 : 'normal')};
  ${(p) => (!p.fitted ? `margin-bottom: ${DefaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}
`

export const H5 = styled.h4<HeadingProps>`
  color: ${Greyscale.dark};
  font-size: ${(p) => (p.smaller ? '14px' : '16px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${(p) => (p.bold ? 600 : 'normal')};
  ${(p) => (!p.fitted ? `margin-bottom: ${DefaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}
`

interface LabelProps {
  inputRow?: boolean
}
export const Label = styled.label<LabelProps>`
  font-weight: 600;
  ${(p) => (p.inputRow ? 'margin-top: 6px;' : '')}
`

interface ParagraphProps {
  fitted?: boolean
  centered?: boolean
}
export const P = styled.p<ParagraphProps>`
  margin-bottom: ${(p) => (!p.fitted ? `${DefaultMargins.s};` : '0')};
  ${(p) => (p.centered ? 'text-align: center;' : '')};
`
