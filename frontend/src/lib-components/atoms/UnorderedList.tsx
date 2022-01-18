// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { defaultMargins, isSpacingSize, SpacingSize } from '../white-space'

export default styled.ul<{ spacing?: SpacingSize | string }>`
  margin: 0;
  padding: 0 0 0 1.5em;

  li {
    &::marker {
      color: ${(p) => p.theme.colors.main.m3};
    }
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
