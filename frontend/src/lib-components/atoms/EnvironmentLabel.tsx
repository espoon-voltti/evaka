// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { tabletMin } from '../breakpoints'

export const EnvironmentLabel = styled.div`
  position: absolute;
  left: 0;
  top: 0;
  z-index: 100;
  background-color: ${(p) => p.theme.colors.status.success};
  color: ${(p) => p.theme.colors.grayscale.g0};
  padding: 4px 12px;
  border-radius: 0 0 12px 0;

  @media (max-width: ${tabletMin}) {
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 0 0 8px 0;
  }
`
