// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { tabletMin } from './breakpoints'

export type SpacingSize =
  | 'zero'
  | 'xxs'
  | 'xs'
  | 's'
  | 'm'
  | 'L'
  | 'XL'
  | 'XXL'
  | 'X3L'
  | 'X4L'

export function isSpacingSize(x: unknown): x is SpacingSize {
  return (
    x === 'zero' ||
    x === 'xxs' ||
    x === 'xs' ||
    x === 's' ||
    x === 'm' ||
    x === 'L' ||
    x === 'XL' ||
    x === 'XXL' ||
    x === 'X3L' ||
    x === 'X4L'
  )
}

export const defaultMargins: Record<SpacingSize, string> = {
  zero: '0px',
  xxs: '4px',
  xs: '8px',
  s: '16px',
  m: '24px',
  L: '32px',
  XL: '48px',
  XXL: '60px',
  X3L: '80px',
  X4L: '120px'
}

type GapProps = {
  horizontal?: boolean
  size?: SpacingSize
  sizeOnMobile?: SpacingSize
}

export const Gap = styled.div<GapProps>`
  display: ${(p) => (p.horizontal ? 'inline-block' : 'block')};
  ${(p) => (p.horizontal ? 'width' : 'height')}: ${(p) =>
    defaultMargins[p.size || 'm']};

  @media (min-width: ${tabletMin}) {
    ${(p) => (p.horizontal ? 'width' : 'height')}: ${(p) =>
      defaultMargins[p.sizeOnMobile || p.size || 'm']};
  }
`
