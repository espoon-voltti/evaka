// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Property } from 'csstype'
import styled from 'styled-components'

interface FlexRowProps {
  justifyContent?: Property.JustifyContent
}

export const FlexRow = styled.div<FlexRowProps>`
  display: flex;
  flex-direction: row;
  width: 100%;
  align-items: baseline;
  ${(p) => (p.justifyContent ? `justify-content: ${p.justifyContent};` : '')}

  > * {
    margin-right: 8px;

    &:last-child {
      margin-right: 0;
    }
  }
`
