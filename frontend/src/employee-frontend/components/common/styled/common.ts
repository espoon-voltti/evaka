// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import {
  defaultMargins,
  isSpacingSize,
  SpacingSize
} from '@evaka/lib-components/white-space'

export const LabelText = styled.span`
  font-weight: 600;
  font-size: 16px;
`

export const Label = styled.label`
  margin-bottom: 8px;
`

export const UnorderedList = styled.ul<{ spacing?: SpacingSize | string }>`
  list-style: none;
  padding: 0;

  li {
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
