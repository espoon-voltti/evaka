// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { CitizenCustomizations } from 'lib-customizations/types'
import EspooLogo from './espoo-logo.svg'
import enCustomizations from './enCustomizations'
import fiCustomizations from './fiCustomizations'
import svCustomizations from './svCustomizations'
import featureFlags from './featureFlags'

const customizations: CitizenCustomizations = {
  fiCustomizations,
  enCustomizations,
  svCustomizations,
  cityLogo: {
    src: EspooLogo,
    alt: 'Espoo Logo'
  },
  featureFlags
}

export default customizations
