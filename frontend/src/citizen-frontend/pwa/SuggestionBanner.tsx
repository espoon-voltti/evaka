// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { desktopMin } from 'lib-components/breakpoints'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { headerHeightMobile } from '../navigation/const'

/** The card pinned below the header that suggests installing the app or enabling push */
export const Banner = styled.div`
  position: sticky;
  top: ${headerHeightMobile}px;
  z-index: 10;
  background-color: ${(p) => p.theme.colors.main.m4};
  padding: ${defaultMargins.s};

  @media (min-width: ${desktopMin}) {
    top: 0;
  }

  html[data-standalone] & {
    top: 0;
  }
`

/** The one-line note that replaces the card after "Myöhemmin" */
export const Note = styled.button`
  width: 100%;
  text-align: left;
  padding: 0;
  border: none;
  background: none;
  font: inherit;
  color: inherit;
  cursor: pointer;
`

export const Row = styled.div`
  display: flex;
  align-items: flex-start;
  gap: ${defaultMargins.s};
`

export const Texts = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: ${defaultMargins.xs};
  flex-grow: 1;
`

export const Title = styled.span`
  font-weight: ${fontWeights.semibold};
`

export const Panel = styled.div`
  margin-top: ${defaultMargins.s};
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  border-radius: 8px;
`
