// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { SelfPosition } from 'csstype'
import {
  DefaultMargins,
  isSpacingSize,
  SpacingSize
} from '~components/shared/layout/white-space'

interface FixedSpaceRowProps {
  spacing?: SpacingSize | string
  justifyContent?: string
  alignItems?: SelfPosition
  marginBottom?: SpacingSize | string
  wrap?: boolean
}
export const FixedSpaceRow = styled.div<FixedSpaceRowProps>`
  display: flex;
  flex-direction: row;
  ${(p) => (p.justifyContent ? `justify-content: ${p.justifyContent};` : '')}
  ${(p) => (p.alignItems ? `align-items: ${p.alignItems};` : '')}
  ${(p) => (p.wrap ? 'flex-wrap: wrap;' : '')}

  ${(p) =>
    p.marginBottom
      ? `margin-bottom: ${
          isSpacingSize(p.marginBottom)
            ? DefaultMargins[p.marginBottom]
            : p.marginBottom
        };`
      : ''}

  >* {
    margin-right: ${(p) =>
      p.spacing
        ? isSpacingSize(p.spacing)
          ? DefaultMargins[p.spacing]
          : p.spacing
        : DefaultMargins.s};
    &:last-child {
      margin-right: 0;
    }
  }

  > button {
    margin-right: ${(p) =>
      p.spacing
        ? isSpacingSize(p.spacing)
          ? DefaultMargins[p.spacing]
          : p.spacing
        : DefaultMargins.s};
    &:last-child {
      margin-right: 0;
    }
  }
`

interface FixedSpaceColumnProps {
  spacing?: SpacingSize | string
  alignItems?: SelfPosition
  marginRight?: SpacingSize | string
}
export const FixedSpaceColumn = styled.div<FixedSpaceColumnProps>`
  display: flex;
  flex-direction: column;
  ${(p) => (p.alignItems ? `align-items: ${p.alignItems};` : '')}

  ${(p) =>
    p.marginRight
      ? `margin-right: ${
          isSpacingSize(p.marginRight)
            ? DefaultMargins[p.marginRight]
            : p.marginRight
        };`
      : ''}

  >* {
    margin-bottom: ${(p) =>
      p.spacing
        ? isSpacingSize(p.spacing)
          ? DefaultMargins[p.spacing]
          : p.spacing
        : DefaultMargins.s};
    &:last-child {
      margin-bottom: 0;
    }
  }
`
