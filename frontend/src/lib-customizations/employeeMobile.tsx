// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export { appConfig } from '@evaka/customizations/employeeMobile'
import {
  featureFlags as baseFeatureFlags,
  translations as baseTranslations
} from '@evaka/customizations/employeeMobile'
import mergeWith from 'lodash/mergeWith'

import { mergeCustomizer } from './common'
import { fi } from './defaults/employee-mobile-frontend/i18n/fi'
import type { FeatureFlags } from './types'

const overrides =
  typeof window !== 'undefined' ? window.evaka?.overrides : undefined

const featureFlags: FeatureFlags = mergeWith(
  {},
  baseFeatureFlags,
  overrides?.featureFlags,
  mergeCustomizer
)
export { featureFlags }

export type Lang = 'fi'
export type Translations = typeof fi

export const translations: { [K in Lang]: Translations } = {
  fi: mergeWith({}, fi, baseTranslations.fi, mergeCustomizer)
}
