// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { blueColors, greyscale } from '@evaka/lib-components/src/colors'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
import { BaseProps } from './utils'

type HeadingProps = BaseProps & {
  fitted?: boolean
  'data-qa'?: string
  centered?: boolean
  noMargin?: boolean
  smaller?: boolean
  bold?: boolean
}

export const H1 = styled.h1<HeadingProps>`
  color: ${blueColors.dark};
  font-size: ${(p) => (p.smaller ? '24px' : '36px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${(p) => (p.bold ? 600 : 200)};
  line-height: 58px;
  ${(p) => (!p.fitted ? `margin-bottom: ${defaultMargins.m};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}

  @media (max-width: 600px) {
    font-size: 24px;
    font-weight: 600;
    line-height: 36px;
  }
`

export const H2 = styled.h2<HeadingProps>`
  color: ${greyscale.dark};
  font-size: ${(p) => (p.smaller ? '20px' : '24px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${(p) => (p.bold ? 600 : 300)};
  ${(p) => (!p.fitted ? `margin-bottom: ${defaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}
`

export const H3 = styled.h3<HeadingProps>`
  color: ${greyscale.dark};
  font-size: ${(p) => (p.smaller ? '18px' : '20px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${(p) => (p.bold ? 600 : 'normal')};
  ${(p) => (!p.fitted ? `margin-bottom: ${defaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}
`

export const H4 = styled.h4<HeadingProps>`
  color: ${greyscale.dark};
  font-size: ${(p) => (p.smaller ? '16px' : '18px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${(p) => (p.bold ? 600 : 'normal')};
  ${(p) => (!p.fitted ? `margin-bottom: ${defaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}
`

export const H5 = styled.h4<HeadingProps>`
  color: ${greyscale.dark};
  font-size: ${(p) => (p.smaller ? '14px' : '16px')};
  font-family: Montserrat, sans-serif;
  font-weight: ${(p) => (p.bold ? 600 : 'normal')};
  ${(p) => (!p.fitted ? `margin-bottom: ${defaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
  ${(p) => (p.noMargin ? `margin: 0;` : '')}
`

type LabelProps = {
  inputRow?: boolean
}

export const Label = styled.label<LabelProps>`
  font-weight: 600;
  ${(p) => (p.inputRow ? 'margin-top: 6px;' : '')}
`

type ParagraphProps = {
  fitted?: boolean
  centered?: boolean
  width?: string
}

export const P = styled.p<ParagraphProps>`
  margin-bottom: ${(p) => (!p.fitted ? `${defaultMargins.s};` : '0')};
  ${(p) => (p.centered ? 'text-align: center;' : '')};
  max-width: ${(p) => p.width || 'auto'};

  strong {
    font-weight: 600;
  }
`
export const Dimmed = styled.span`
  color: ${greyscale.medium};
`
