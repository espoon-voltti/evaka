// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import {
  DefaultMargins,
  SpacingSize
} from 'components/shared/layout/white-space'
import { BaseProps } from 'components/shared/utils'

export const Container = styled.div`
  margin: 0 auto;
  position: relative;

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
`

interface ContentAreaProps extends BaseProps {
  opaque: boolean
  paddingVertical?: SpacingSize
  paddingHorozontal?: SpacingSize
}

export const ContentArea = styled.section<ContentAreaProps>`
  padding: ${(p) =>
    `${
      p.paddingVertical ? DefaultMargins[p.paddingVertical] : DefaultMargins.s
    } ${
      p.paddingHorozontal
        ? DefaultMargins[p.paddingHorozontal]
        : DefaultMargins.L
    }`};
  background-color: ${(props) => (props.opaque ? 'white' : 'transparent')};
  position: relative;
`

export default Container
