// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { Container } from 'lib-components/layout/Container'
import type { SpacingSize } from 'lib-components/white-space'
import { defaultMargins } from 'lib-components/white-space'

type VasuContainerProps = {
  gapSize: SpacingSize
}

export const VasuContainer = styled(Container)<VasuContainerProps>`
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: ${(p) => defaultMargins[p.gapSize]};

  @media print {
    display: block;
    * {
      overflow: visible;
      text-overflow: visible;
    }

    section {
      display: block;
      break-inside: avoid-page;
      break-before: auto;
    }
  }
`
