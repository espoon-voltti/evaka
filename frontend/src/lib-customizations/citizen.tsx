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
import { mergeWith } from 'lodash'
import { translationsMergeCustomizer } from './common'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const {
  appConfig,
  cityLogo,
  featureFlags,
  footerLogo,
  langs,
  mapConfig,
  routeLinkRootUrl
}: CitizenCustomizations = customizations
export {
  appConfig,
  cityLogo,
  featureFlags,
  footerLogo,
  langs,
  mapConfig,
  routeLinkRootUrl
}

export type Lang = 'fi' | 'sv' | 'en'

export type Translations = typeof fi

export const translations: Record<Lang, Translations> = {
  fi: mergeWith(
    fi,
    (customizations as CitizenCustomizations).translations.fi,
    translationsMergeCustomizer
  ),
  sv: mergeWith(
    sv,
    (customizations as CitizenCustomizations).translations.sv,
    translationsMergeCustomizer
  ),
  en: mergeWith(
    en,
    (customizations as CitizenCustomizations).translations.en,
    translationsMergeCustomizer
  )
}
