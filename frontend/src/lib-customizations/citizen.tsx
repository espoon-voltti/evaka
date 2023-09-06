// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export {
  appConfig,
  cityLogo,
  footerLogo,
  mapConfig,
  routeLinkRootUrl,
  getMaxPreferredUnits
} from '@evaka/customizations/citizen'
import {
  langs as baseLangs,
  featureFlags as baseFeatureFlags,
  translations as baseTranslations
} from '@evaka/customizations/citizen'
import mergeWith from 'lodash/mergeWith'

import { mergeCustomizer } from './common'
import en from './defaults/citizen/i18n/en'
import fi from './defaults/citizen/i18n/fi'
import sv from './defaults/citizen/i18n/sv'
import type { CitizenModule, FeatureFlags } from './types'

const overrides =
  typeof window !== 'undefined' ? window.evaka?.overrides : undefined

const langs: CitizenModule['langs'] = overrides?.citizen?.langs ?? baseLangs
const featureFlags: FeatureFlags = mergeWith(
  {},
  baseFeatureFlags,
  overrides?.featureFlags,
  mergeCustomizer
)
export { featureFlags, langs }

export type Lang = 'fi' | 'sv' | 'en'

export type Translations = typeof fi

export const translations: Record<Lang, Translations> = {
  fi: mergeWith({}, fi, baseTranslations.fi, mergeCustomizer),
  sv: mergeWith({}, sv, baseTranslations.sv, mergeCustomizer),
  en: mergeWith({}, en, baseTranslations.en, mergeCustomizer)
}
