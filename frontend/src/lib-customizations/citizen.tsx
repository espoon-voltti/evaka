// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import defaultsUntyped from '@evaka/customizations/citizen'
import mergeWith from 'lodash/mergeWith'

import { JsonOf } from 'lib-common/json'

import { mergeCustomizer } from './common'
import en from './defaults/citizen/i18n/en'
import fi from './defaults/citizen/i18n/fi'
import sv from './defaults/citizen/i18n/sv'
import type { CitizenCustomizations } from './types'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const defaults: CitizenCustomizations = defaultsUntyped

declare global {
  interface EvakaWindowConfig {
    citizenCustomizations?: Partial<JsonOf<CitizenCustomizations>>
  }
}

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const overrides: Partial<JsonOf<CitizenCustomizations>> | undefined =
  typeof window !== 'undefined'
    ? {
        ...window.evaka?.citizenCustomizations,
        ...JSON.parse(
          sessionStorage.getItem('evaka.citizenCustomizations') || '{}'
        )
      }
    : undefined

const customizations: CitizenCustomizations = overrides
  ? mergeWith({}, defaults, overrides, mergeCustomizer)
  : defaults

const {
  appConfig,
  cityLogo,
  featureFlags,
  footerLogo,
  langs,
  mapConfig,
  routeLinkRootUrl,
  getMaxPreferredUnits
}: CitizenCustomizations = customizations
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
