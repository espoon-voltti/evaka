// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { tabletMin } from 'lib-components/breakpoints'

export const BottomFooterContainer = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;

  & > *:nth-child(1) {
    flex-grow: 1;

    @media (max-width: ${tabletMin}) {
      min-height: 0;
      overflow: auto;
    }
  }

  & > *:nth-child(2) {
    flex-shrink: 0;
  }
`
