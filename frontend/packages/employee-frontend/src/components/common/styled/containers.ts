// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { ContentDistribution, ContentPosition } from 'csstype'

export const DivFitContent = styled.div`
  width: fit-content;
`

interface FlexRowProps {
  justifyContent?: ContentDistribution | ContentPosition
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
