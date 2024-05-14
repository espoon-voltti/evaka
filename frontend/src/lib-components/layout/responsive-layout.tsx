// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { useWindowSize } from 'lib-common/utils/useWindowSize'

import {
  desktopMin,
  desktopMinPx,
  tabletMin,
  tabletMinPx
} from '../breakpoints'

export const MobileOnly = styled.div`
  display: block;
  @media (min-width: ${tabletMin}) {
    display: none;
  }
`

export const MobileAndTablet = styled.div`
  display: block;
  @media (min-width: ${desktopMin}) {
    display: none;
  }
`

export const TabletAndDesktop = styled.div`
  display: none;
  @media (min-width: ${tabletMin}) {
    display: block;
  }
`

export const Desktop = styled.div`
  display: none;
  @media (min-width: ${desktopMin}) {
    display: block;
  }
`

export const RenderOnlyOn = (props: {
  mobile?: boolean
  tablet?: boolean
  desktop?: boolean
  children: React.ReactNode
}) => {
  const windowSize = useWindowSize()
  const isMobile = windowSize.width < tabletMinPx
  const isTablet =
    windowSize.width >= tabletMinPx && windowSize.width < desktopMinPx
  const isDesktop = windowSize.width >= desktopMinPx

  const shouldRender =
    !!(props.mobile && isMobile) ||
    !!(props.tablet && isTablet) ||
    !!(props.desktop && isDesktop)

  return shouldRender ? <>{props.children}</> : null
}
