// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { EmployeeCustomizations } from 'lib-customizations/types'
import EspooLogo from './EspooLogo.png'
import featureFlags from './featureFlags'
import { fi } from './employee/assets/i18n/fi'

const customizations: EmployeeCustomizations = {
  translations: {
    fi
  },
  cityLogo: {
    src: EspooLogo,
    alt: 'Espoo Logo'
  },
  featureFlags
}

export default customizations
