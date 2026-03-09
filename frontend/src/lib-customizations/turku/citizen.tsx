{
  /*
SPDX-FileCopyrightText: 2021 City of Turku

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}
import React from 'react'
import styled from 'styled-components'

import { desktopMin, tabletMin } from 'lib-components/breakpoints'
import type { CitizenCustomizations } from 'lib-customizations/types'

import TurkuLogo from './city-logo-citizen.png'
import enCustomizations from './enCustomizations'
import featureFlags from './featureFlags'
import fiCustomizations from './fiCustomizations'
import mapConfig from './mapConfig'
import svCustomizations from './svCustomizations'

const TurkuFooterLogo = styled.div`
  margin-top: 10px;
  text-align: center;

  @media (min-width: ${tabletMin}) {
    margin-top: 0;
    flex-basis: 100%;
  }

  @media (min-width: ${desktopMin}) {
    flex-basis: auto;
    margin-top: -20px;
    text-align: end;
  }

  @media (min-width: 1408px) {
    flex-basis: auto;
    margin-top: -20px;
    text-align: end;
  }
`

const TurkuFooterImg = styled.img`
  width: auto;

  @media (min-width: ${desktopMin}) {
    height: 70px;
  }
`

const TurkuFooter = (
  <TurkuFooterLogo>
    <TurkuFooterImg src={TurkuLogo} alt="Turku footer logo" />
  </TurkuFooterLogo>
)

const customizations: CitizenCustomizations = {
  appConfig: {},
  langs: ['fi', 'sv', 'en'],
  translations: {
    fi: fiCustomizations,
    sv: svCustomizations,
    en: enCustomizations
  },
  cityLogo: {
    src: TurkuLogo,
    alt: 'Turku logo'
  },
  footerLogo: TurkuFooter,
  routeLinkRootUrl: 'https://turku.digitransit.fi/',
  mapConfig,
  featureFlags,
  getMaxPreferredUnits() {
    return 3
  }
}

export default customizations
