// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export { citizenConfig as appConfig } from './appConfigs'
import { CitizenModule } from 'lib-customizations/types'

import en from '../defaults/citizen/i18n/en'
import fi from '../defaults/citizen/i18n/fi'
import sv from '../defaults/citizen/i18n/sv'

import EspooLogo from './assets/EspooLogoBlue.svg'

export { featureFlags } from './featureFlags'
export { mapConfig } from './mapConfig'

export const langs: CitizenModule['langs'] = ['fi', 'sv', 'en']
export const translations: CitizenModule['translations'] = {
  fi,
  sv,
  en
}

export const footerLogo: CitizenModule['footerLogo'] = undefined

export const cityLogo: CitizenModule['cityLogo'] = {
  src: EspooLogo,
  alt: 'Espoo Logo'
}

export const routeLinkRootUrl = 'https://reittiopas.hsl.fi/reitti/'

export const getMaxPreferredUnits = () => 3
