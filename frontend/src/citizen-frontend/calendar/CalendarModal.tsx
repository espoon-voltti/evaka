// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { tabletMin } from 'lib-components/breakpoints'
import { defaultMargins } from 'lib-components/white-space'

export const CalendarModalCloseButton = styled(IconOnlyButton)`
  position: absolute;
  top: ${defaultMargins.s};
  right: ${defaultMargins.s};
  color: ${(p) => p.theme.colors.main.m2};
`

export const CalendarModalButtons = styled.div`
  display: flex;
  justify-content: space-between;
  gap: ${defaultMargins.s};
  padding: ${defaultMargins.L};

  @media (max-width: ${tabletMin}) {
    padding: ${defaultMargins.s};
    display: grid;
    grid-template-columns: 1fr 1fr;
    background-color: ${(p) => p.theme.colors.main.m4};
  }
`

export const CalendarModalSection = styled.section`
  padding: 0 ${defaultMargins.L};

  @media (max-width: ${tabletMin}) {
    padding: ${defaultMargins.m} ${defaultMargins.s};
    background-color: ${(p) => p.theme.colors.grayscale.g0};

    h2 {
      margin-top: 0;
    }
  }
`

export const CalendarModalBackground = styled.div`
  height: 100%;

  @media (max-width: ${tabletMin}) {
    background-color: ${(p) => p.theme.colors.main.m4};
  }
`
