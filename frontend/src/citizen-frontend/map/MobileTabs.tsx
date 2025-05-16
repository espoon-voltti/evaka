// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { stickyElementZIndex } from 'lib-components/layout/z-helpers'
import { fontWeights } from 'lib-components/typography'
import colors from 'lib-customizations/common'

import { useTranslation } from '../localization'
import { headerHeightMobile } from '../navigation/const'

import type { MobileMode } from './const'
import { mapViewBreakpoint } from './const'

const MobileTabContainer = styled.div`
  display: none;
  @media (max-width: ${mapViewBreakpoint}) {
    display: flex;
    justify-content: stretch;
    height: ${headerHeightMobile}px;
    background-color: ${colors.grayscale.g0};

    position: sticky;
    top: 60px;
    z-index: ${stickyElementZIndex};
  }
`

const Tab = styled.div<{ active: boolean }>`
  flex: 1 0 0;
  display: flex;
  justify-content: center;
  align-items: center;
  border-bottom: 3px solid ${(p) => (p.active ? colors.main.m2 : 'transparent')};
  cursor: pointer;

  color: ${colors.main.m2};
  font-family: 'Montserrat', 'Open Sans', sans-serif;
  font-size: 14px;
  text-transform: uppercase;
  font-weight: ${(p) => (p.active ? fontWeights.bold : fontWeights.medium)};
`

interface Props {
  mobileMode: MobileMode
  setMobileMode: (mode: MobileMode) => void
}

export default React.memo(function MobileTabs({
  mobileMode,
  setMobileMode
}: Props) {
  const t = useTranslation()
  return (
    <MobileTabContainer>
      <Tab onClick={() => setMobileMode('map')} active={mobileMode === 'map'}>
        {t.map.mobileTabs.map}
      </Tab>
      <Tab onClick={() => setMobileMode('list')} active={mobileMode === 'list'}>
        {t.map.mobileTabs.list}
      </Tab>
    </MobileTabContainer>
  )
})
