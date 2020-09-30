// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { Greyscale } from 'components/shared/Colors'
import { DefaultMargins } from 'components/shared/layout/white-space'

interface HeadingProps {
  fitted?: boolean
  'data-qa'?: string
  centered?: boolean
}

export const H1 = styled.h1<HeadingProps>`
  color: ${Greyscale.dark};
  font-size: 36px;
  font-family: Montserrat, sans-serif;
  font-weight: 200;
  line-height: 58px;
  ${(p) => (!p.fitted ? `margin-bottom: ${DefaultMargins.m};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
`

export const H2 = styled.h2<HeadingProps>`
  color: ${Greyscale.dark};
  font-size: 24px;
  font-family: Montserrat, sans-serif;
  font-weight: 300;
  ${(p) => (!p.fitted ? `margin-bottom: ${DefaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
`

export const H3 = styled.h3<HeadingProps>`
  color: ${Greyscale.dark};
  font-size: 20px;
  font-family: Montserrat, sans-serif;
  font-weight: normal;
  ${(p) => (!p.fitted ? `margin-bottom: ${DefaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
`

export const H4 = styled.h4<HeadingProps>`
  color: ${Greyscale.dark};
  font-size: 18px;
  font-family: Montserrat, sans-serif;
  font-weight: normal;
  ${(p) => (!p.fitted ? `margin-bottom: ${DefaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
`

export const H5 = styled.h4<HeadingProps>`
  color: ${Greyscale.dark};
  font-size: 16px;
  font-family: Montserrat, sans-serif;
  font-weight: normal;
  ${(p) => (!p.fitted ? `margin-bottom: ${DefaultMargins.s};` : '')}
  ${(p) => (p.centered ? `text-align: center;` : '')}
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
}
export const P = styled.p<ParagraphProps>`
  ${(p) => (!p.fitted ? `margin-bottom: ${DefaultMargins.s};` : '')}
`
