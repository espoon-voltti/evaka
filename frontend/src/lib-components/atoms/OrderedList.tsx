// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { defaultMargins, type SpacingSize } from '../white-space'

export default styled.ol<{ spacing?: SpacingSize }>`
  margin: 0;
  padding: 0 0 0 1.5em;

  li {
    margin-bottom: ${(p) =>
      p.spacing ? defaultMargins[p.spacing] : defaultMargins.s};

    &:last-child {
      margin-bottom: 0;
    }
  }
`
