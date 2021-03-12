// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { CitizenCustomizations } from '@evaka/lib-customizations/types'
import EspooLogo from './espoo-logo.svg'

const customizations: CitizenCustomizations = {
  cityLogo: {
    src: EspooLogo,
    alt: 'Espoo Logo'
  }
}

export default customizations
