{
  /*
SPDX-FileCopyrightText: 2021 City of Tampere

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import styled from 'styled-components'

import { desktopMin, tabletMin } from 'lib-components/breakpoints'
import type { CitizenCustomizations } from 'lib-customizations/types'

import TampereLogo from './city-logo-citizen.svg'
import enCustomizations from './enCustomizations'
import featureFlags from './featureFlags'
import fiCustomizations from './fiCustomizations'
import mapConfig from './mapConfig'
import TampereFinlandLogoImg from './tampere.finland.svg'
import TampereWavesImg from './tampere_aallot.svg'

const TreFooterLogo = styled.div`
  height: 60px;
  margin-top: 10px;
  text-align: end;

  @media (min-width: ${tabletMin}) {
    margin-top: 0;
    flex-basis: 100%;
  }

  @media (min-width: ${desktopMin}) {
    margin-top: 32px;
  }

  @media (min-width: 1408px) {
    flex-basis: auto;
    margin-top: -10px;
    padding-left: 40px;
    margin-right: -64px;
  }
`

const TampereFinlandLogo = styled.img`
  vertical-align: top;
  height: 60px;
  width: 157px;
  margin-right: -32px;
  margin-top: 5px;

  @media (min-width: ${tabletMin}) {
    margin-top: -10px;
  }

  @media (min-width: ${desktopMin}) {
    margin-top: 5px;
  }

  @media (min-width: 1408px) {
    margin-right: -41px;
    margin-top: 0;
  }
`

const TampereAaltoLogoSvg = styled.img`
  margin-top: 0;
  height: 97px;
  width: 204px;

  @media (min-width: ${tabletMin}) {
    margin-top: -15px;
  }

  @media (min-width: ${desktopMin}) {
    margin-top: 0;
    margin-right: -64px;
  }

  @media (min-width: 1408px) {
    margin-right: 0;
    margin-top: -5px;
  }
`

const TampereFooter = (
  <TreFooterLogo>
    <TampereFinlandLogo
      src={TampereFinlandLogoImg}
      alt="Tampere.Finland logo"
    />
    <TampereAaltoLogoSvg src={TampereWavesImg} alt="Tampere - aalto logo" />
  </TreFooterLogo>
)

const customizations: CitizenCustomizations = {
  appConfig: {},
  langs: ['fi', 'en'],
  translations: {
    fi: fiCustomizations,
    sv: {},
    en: enCustomizations
  },
  cityLogo: {
    src: TampereLogo,
    alt: 'Tampere logo'
  },
  footerLogo: TampereFooter,
  routeLinkRootUrl: 'https://reittiopas.tampere.fi/reitti/',
  mapConfig,
  featureFlags,
  getMaxPreferredUnits(type) {
    if (type === 'PRESCHOOL') {
      return 1
    }
    return 3
  }
}

export default customizations
