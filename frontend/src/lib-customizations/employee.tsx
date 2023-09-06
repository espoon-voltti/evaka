// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  featureFlags as baseFeatureFlags,
  translations as baseTranslations,
  vasuTranslations as baseVasuTranslations
} from '@evaka/customizations/employee'
import mergeWith from 'lodash/mergeWith'

import { ApplicationType } from 'lib-common/generated/api-types/application'

import { mergeCustomizer } from './common'
import { fi } from './defaults/employee/i18n/fi'
import { sv } from './defaults/employee/i18n/sv'
import { fi as vasuFI } from './defaults/employee/i18n/vasu/fi'
import { sv as vasuSV } from './defaults/employee/i18n/vasu/sv'
import { FeatureFlags } from './types'

export {
  appConfig,
  cityLogo,
  absenceTypes,
  assistanceMeasures,
  daycareAssistanceLevels,
  otherAssistanceMeasureTypes,
  preschoolAssistanceLevels,
  placementTypes,
  placementPlanRejectReasons,
  unitProviderTypes,
  voucherValueDecisionTypes
} from '@evaka/customizations/employee'

const overrides =
  typeof window !== 'undefined' ? window.evaka?.overrides : undefined

const featureFlags: FeatureFlags = mergeWith(
  {},
  baseFeatureFlags,
  overrides?.featureFlags,
  mergeCustomizer
)
export { featureFlags }

export type Lang = 'fi' | 'sv'

export type Translations = typeof fi

export const translations: { [K in Lang]: Translations } = {
  fi: mergeWith({}, fi, baseTranslations.fi, mergeCustomizer),
  sv: mergeWith({}, sv, baseTranslations.sv, mergeCustomizer)
}

export type VasuLang = 'FI' | 'SV'
export type VasuTranslations = typeof vasuFI

export const vasuTranslations: { [K in VasuLang]: VasuTranslations } = {
  FI: mergeWith({}, vasuFI, baseVasuTranslations.FI, mergeCustomizer),
  SV: mergeWith(vasuSV, baseVasuTranslations.SV, mergeCustomizer)
}

export const applicationTypes: ApplicationType[] = (
  ['DAYCARE', 'PRESCHOOL', 'CLUB'] as const
).filter((type) => featureFlags.preschool || type !== 'PRESCHOOL')
