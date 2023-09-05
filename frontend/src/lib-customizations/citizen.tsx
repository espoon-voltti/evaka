// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import customizationsUntyped from '@evaka/customizations/citizen'
import mergeWith from 'lodash/mergeWith'

import { mergeCustomizer } from './common'
import en from './defaults/citizen/i18n/en'
import fi from './defaults/citizen/i18n/fi'
import sv from './defaults/citizen/i18n/sv'
import type { CitizenCustomizations } from './types'
import { FeatureFlags } from './types'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const customizations: CitizenCustomizations = customizationsUntyped

const overrides =
  typeof window !== 'undefined' ? window.evaka?.overrides : undefined

const {
  appConfig,
  cityLogo,
  footerLogo,
  mapConfig,
  routeLinkRootUrl,
  getMaxPreferredUnits
}: CitizenCustomizations = customizations
const langs: CitizenCustomizations['langs'] =
  overrides?.citizen?.langs ?? customizations.langs
const featureFlags: FeatureFlags = mergeWith(
  {},
  customizations.featureFlags,
  overrides?.featureFlags,
  mergeCustomizer
)
export {
  appConfig,
  cityLogo,
  featureFlags,
  footerLogo,
  langs,
  mapConfig,
  routeLinkRootUrl,
  getMaxPreferredUnits
}

export type Lang = 'fi' | 'sv' | 'en'

export type Translations = typeof fi

export const translations: Record<Lang, Translations> = {
  fi: mergeWith({}, fi, customizations.translations.fi, mergeCustomizer),
  sv: mergeWith({}, sv, customizations.translations.sv, mergeCustomizer),
  en: mergeWith({}, en, customizations.translations.en, mergeCustomizer)
}
