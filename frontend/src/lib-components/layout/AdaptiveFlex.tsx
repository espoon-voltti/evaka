// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { tabletMin } from '../breakpoints'
import type { SpacingSize } from '../white-space'
import { defaultMargins } from '../white-space'

type AdaptiveFlexProps = {
  breakpoint?: string
  horizontalSpacing?: SpacingSize
  verticalSpacing?: SpacingSize
}

const AdaptiveFlex = styled.div<AdaptiveFlexProps>`
  display: flex;
  flex-direction: row;
  align-items: center;

  > * {
    margin-right: ${(p) =>
      p.horizontalSpacing
        ? defaultMargins[p.horizontalSpacing]
        : defaultMargins.s};
    &:last-child {
      margin-right: 0;
    }
  }

  @media (max-width: ${(p) => p.breakpoint ?? tabletMin}) {
    flex-direction: column;

    > * {
      width: 100%;
      margin-right: 0;
      margin-bottom: ${(p) =>
        p.verticalSpacing
          ? defaultMargins[p.verticalSpacing]
          : defaultMargins.s};
      &:last-child {
        margin-bottom: 0;
      }
    }
  }
`

export default AdaptiveFlex
