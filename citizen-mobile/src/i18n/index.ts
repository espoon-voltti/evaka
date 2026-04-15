// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Localization from 'expo-localization'
import { I18n } from 'i18n-js'

import en from './en'
import fi from './fi'
import sv from './sv'

export type Language = 'fi' | 'sv' | 'en'

export const i18n = new I18n({ fi, sv, en })
i18n.enableFallback = true
i18n.defaultLocale = 'fi'

const deviceLocale = Localization.getLocales()[0]?.languageCode ?? 'fi'
i18n.locale = (
  ['fi', 'sv', 'en'].includes(deviceLocale) ? deviceLocale : 'fi'
) as Language

export function setLanguage(lang: Language) {
  i18n.locale = lang
}

export function t(key: string, opts?: Record<string, unknown>): string {
  return i18n.t(key, opts)
}
