// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import customizationsUntyped from '@evaka/customizations/employee'
import mergeWith from 'lodash/mergeWith'

import { ApplicationType } from 'lib-common/generated/api-types/application'

import { mergeCustomizer } from './common'
import { fi } from './defaults/employee/i18n/fi'
import { sv } from './defaults/employee/i18n/sv'
import { fi as vasuFI } from './defaults/employee/i18n/vasu/fi'
import { sv as vasuSV } from './defaults/employee/i18n/vasu/sv'
import type { EmployeeCustomizations } from './types'
import { FeatureFlags } from './types'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const customizations: EmployeeCustomizations = customizationsUntyped

const overrides =
  typeof window !== 'undefined' ? window.evaka?.overrides : undefined

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const {
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
}: EmployeeCustomizations = customizations
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
  absenceTypes,
  assistanceMeasures,
  daycareAssistanceLevels,
  otherAssistanceMeasureTypes,
  placementTypes,
  placementPlanRejectReasons,
  preschoolAssistanceLevels,
  unitProviderTypes,
  voucherValueDecisionTypes
}

export type Lang = 'fi' | 'sv'

export type Translations = typeof fi

export const translations: { [K in Lang]: Translations } = {
  fi: mergeWith({}, fi, customizations.translations.fi, mergeCustomizer),
  sv: mergeWith({}, sv, customizations.translations.sv, mergeCustomizer)
}

export type VasuLang = 'FI' | 'SV'
export type VasuTranslations = typeof vasuFI

export const vasuTranslations: { [K in VasuLang]: VasuTranslations } = {
  FI: mergeWith(
    {},
    vasuFI,
    customizations.vasuTranslations.FI,
    mergeCustomizer
  ),
  SV: mergeWith(vasuSV, customizations.vasuTranslations.SV, mergeCustomizer)
}

export const applicationTypes: ApplicationType[] = (
  ['DAYCARE', 'PRESCHOOL', 'CLUB'] as const
).filter((type) => featureFlags.preschool || type !== 'PRESCHOOL')
