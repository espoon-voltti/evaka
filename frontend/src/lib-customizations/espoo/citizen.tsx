// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { CitizenCustomizations } from 'lib-customizations/types'
import EspooLogo from './espoo-logo.svg'
import mapConfig from './mapConfig'
import featureFlags from './featureFlags'

const customizations: CitizenCustomizations = {
  translations: {
    fi: {},
    sv: {},
    en: {}
  },
  cityLogo: {
    src: EspooLogo,
    alt: 'Espoo Logo'
  },
  mapConfig,
  featureFlags
}

export default customizations
