// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import customizations from '@evaka/customizations/citizen'
import type { CitizenCustomizations } from './types'
import fi from './espoo/citizen/assets/i18n/fi'
import sv from './espoo/citizen/assets/i18n/sv'
import en from './espoo/citizen/assets/i18n/en'
import { merge } from 'lodash'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const {
  cityLogo,
  mapConfig,
  featureFlags
}: CitizenCustomizations = customizations
export { cityLogo, mapConfig, featureFlags }

export const langs = ['fi', 'sv', 'en'] as const

export type Lang = typeof langs[number]

export type Translations = typeof fi

export const translations: Record<Lang, Translations> = {
  fi: merge(fi, (customizations as CitizenCustomizations).translations.fi),
  sv: merge(sv, (customizations as CitizenCustomizations).translations.sv),
  en: merge(en, (customizations as CitizenCustomizations).translations.en)
}
