// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { tabletMin } from './breakpoints'

export type SpacingSize =
  | 'zero'
  | 'x3s'
  | 'xxs'
  | 'xs'
  | 's'
  | 'm'
  | 'L'
  | 'XL'
  | 'XXL'
  | 'X3L'
  | 'X4L'
  | 'X5L'

export function isSpacingSize(x: unknown): x is SpacingSize {
  return (
    x === 'zero' ||
    x === 'x3s' ||
    x === 'xxs' ||
    x === 'xs' ||
    x === 's' ||
    x === 'm' ||
    x === 'L' ||
    x === 'XL' ||
    x === 'XXL' ||
    x === 'X3L' ||
    x === 'X4L' ||
    x === 'X5L'
  )
}

export const defaultMargins: Record<SpacingSize, string> = {
  zero: '0px',
  x3s: '2px',
  xxs: '4px',
  xs: '8px',
  s: '16px',
  m: '24px',
  L: '32px',
  XL: '40px',
  XXL: '48px',
  X3L: '64px',
  X4L: '80px',
  X5L: '120px'
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
  flex-shrink: 0;

  @media (max-width: ${tabletMin}) {
    ${(p) => (p.horizontal ? 'width' : 'height')}: ${(p) =>
      defaultMargins[p.sizeOnMobile || p.size || 'm']};
  }
`
