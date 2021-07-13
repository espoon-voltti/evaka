// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { CitizenCustomizations } from 'lib-customizations/types'
import EspooLogo from './assets/espoo-logo.svg'
import mapConfig from './mapConfig'
import featureFlags from './featureFlags'

const customizations: CitizenCustomizations = {
  langs: ['fi', 'sv', 'en'],
  translations: {
    fi: {},
    sv: {},
    en: {}
  },
  cityLogo: {
    src: EspooLogo,
    alt: 'Espoo Logo'
  },
  routeLinkRootUrl: 'https://reittiopas.hsl.fi/reitti/',
  mapConfig,
  featureFlags
}

export default customizations
