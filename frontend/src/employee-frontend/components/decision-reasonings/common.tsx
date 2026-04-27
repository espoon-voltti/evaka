// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { defaultMargins } from 'lib-components/white-space'

export const ReasoningCard = styled.div<{
  $state?: 'active' | 'inactive' | 'warning'
}>`
  border: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  border-left: 4px solid
    ${(p) =>
      p.$state === 'inactive'
        ? p.theme.colors.grayscale.g15
        : p.$state === 'warning'
          ? p.theme.colors.status.warning
          : p.theme.colors.main.m1};
  border-radius: 4px;
  padding: ${defaultMargins.m};
`

export const LanguageGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: ${defaultMargins.L};
`

// Custom colors for status chips
export const chipColors = {
  notReady: { background: '#FFEEE0', text: '#A84C00' },
  active: { background: '#DEF2DF', text: '#2A6A2C' }
}

export const PreWrap = styled.span`
  white-space: pre-wrap;
`

export const CollapsibleHeader = styled.button`
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  font-size: 1rem;
  font-weight: 600;
  color: ${(p) => p.theme.colors.main.m2};
  display: flex;
  align-items: center;
  gap: ${defaultMargins.xs};
`
