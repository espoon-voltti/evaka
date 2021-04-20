// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { CitizenCustomizations } from 'lib-customizations/types'
import EspooLogo from './espoo-logo.svg'
import enCustomizations from './enCustomizations'
import fiCustomizations from './fiCustomizations'
import svCustomizations from './svCustomizations'
import mapConfig from './mapConfig'
import featureFlags from './featureFlags'

const customizations: CitizenCustomizations = {
  fiCustomizations,
  enCustomizations,
  svCustomizations,
  cityLogo: {
    src: EspooLogo,
    alt: 'Espoo Logo'
  },
  mapConfig,
  mapSearchAreaRect: {
    maxLatitude: 60.35391259995084,
    minLatitude: 59.9451623086072,
    maxLongitude: 25.32055693401933,
    minLongitude: 24.271362626190594,
  },
  featureFlags
}

export default customizations
