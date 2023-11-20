// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Property } from 'csstype'
import styled from 'styled-components'

import { defaultMargins, isSpacingSize, SpacingSize } from '../white-space'

interface FixedSpaceRowProps {
  // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
  spacing?: SpacingSize | string
  justifyContent?: Property.JustifyContent
  alignItems?: Property.AlignItems
  fullWidth?: boolean
  maxWidth?: string
  flexWrap?: Property.FlexWrap
  gap?: string
}
export const FixedSpaceRow = styled.div<FixedSpaceRowProps>`
  display: flex;
  flex-direction: row;
  ${(p) => (p.justifyContent ? `justify-content: ${p.justifyContent};` : '')}
  ${(p) => (p.alignItems ? `align-items: ${p.alignItems};` : '')}
  ${(p) => (p.fullWidth ? `width: 100%;` : '')}
  ${(p) => (p.flexWrap ? `flex-wrap: ${p.flexWrap};` : '')}
  ${(p) => (p.gap ? `gap: ${p.gap};` : '')}

  >* {
    margin-right: ${(p) =>
      p.spacing
        ? isSpacingSize(p.spacing)
          ? defaultMargins[p.spacing]
          : p.spacing
        : defaultMargins.s};
    &:last-child {
      margin-right: 0;
    }
    ${(p) => (p.maxWidth ? `max-width: ${p.maxWidth};` : '')};
  }

  > button {
    margin-right: ${(p) =>
      p.spacing
        ? isSpacingSize(p.spacing)
          ? defaultMargins[p.spacing]
          : p.spacing
        : defaultMargins.s};
    &:last-child {
      margin-right: 0;
    }
  }
`

interface FixedSpaceColumnProps {
  // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
  spacing?: SpacingSize | string
  justifyContent?: Property.JustifyContent
  alignItems?: Property.AlignItems
  // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
  marginRight?: SpacingSize | string
  fullWidth?: boolean
}

export const FixedSpaceColumn = styled.div<FixedSpaceColumnProps>`
  display: flex;
  flex-direction: column;
  ${(p) => (p.justifyContent ? `justify-content: ${p.justifyContent};` : '')}
  ${(p) => (p.alignItems ? `align-items: ${p.alignItems};` : '')}
  ${(p) => (p.fullWidth ? `width: 100%;` : '')}

  ${(p) =>
    p.marginRight
      ? `margin-right: ${
          isSpacingSize(p.marginRight)
            ? defaultMargins[p.marginRight]
            : p.marginRight
        };`
      : ''}

  >* {
    margin-bottom: ${(p) =>
      p.spacing
        ? isSpacingSize(p.spacing)
          ? defaultMargins[p.spacing]
          : p.spacing
        : defaultMargins.s};
    &:last-child {
      margin-bottom: 0;
    }
  }
`

interface FixedSpaceFlexWrapProps {
  horizontalSpacing?: SpacingSize
  verticalSpacing?: SpacingSize
  reverse?: boolean
}
export const FixedSpaceFlexWrap = styled.div<FixedSpaceFlexWrapProps>`
  display: flex;
  flex-direction: row;
  flex-wrap: ${(p) => (p.reverse ? 'wrap-reverse' : 'wrap')};
  justify-content: flex-start;
  align-items: flex-start;

  margin-bottom: -${(p) => (p.verticalSpacing ? defaultMargins[p.verticalSpacing] : defaultMargins.s)};
  margin-right: -${(p) => (p.horizontalSpacing ? defaultMargins[p.horizontalSpacing] : defaultMargins.s)};

  > * {
    margin-bottom: ${(p) =>
      p.verticalSpacing ? defaultMargins[p.verticalSpacing] : defaultMargins.s};
    margin-right: ${(p) =>
      p.horizontalSpacing
        ? defaultMargins[p.horizontalSpacing]
        : defaultMargins.s};
  }
`
