// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import defaultsUntyped from '@evaka/customizations/employee'
import mergeWith from 'lodash/mergeWith'

import { ApplicationType } from 'lib-common/generated/api-types/application'
import { JsonOf } from 'lib-common/json'

import { mergeCustomizer } from './common'
import { fi } from './espoo/employee/assets/i18n/fi'
import { fi as vasuFI } from './espoo/employee/assets/i18n/vasu/fi'
import { sv as vasuSV } from './espoo/employee/assets/i18n/vasu/sv'
import type { EmployeeCustomizations } from './types'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const defaults: EmployeeCustomizations = defaultsUntyped

declare global {
  interface EvakaWindowConfig {
    employeeCustomizations?: Partial<JsonOf<EmployeeCustomizations>>
  }
}

const overrides =
  typeof window !== 'undefined'
    ? window.evaka?.employeeCustomizations
    : undefined

const customizations: EmployeeCustomizations = overrides
  ? mergeWith({}, defaults, overrides, mergeCustomizer)
  : defaults

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const {
  appConfig,
  cityLogo,
  featureFlags,
  absenceTypes,
  assistanceMeasures,
  placementTypes,
  placementPlanRejectReasons,
  unitProviderTypes,
  voucherValueDecisionTypes
}: EmployeeCustomizations = customizations
export {
  appConfig,
  cityLogo,
  featureFlags,
  absenceTypes,
  assistanceMeasures,
  placementTypes,
  placementPlanRejectReasons,
  unitProviderTypes,
  voucherValueDecisionTypes
}

export type Lang = 'fi'

export type Translations = typeof fi

export const translations: { [K in Lang]: Translations } = {
  fi: mergeWith({}, fi, customizations.translations.fi, mergeCustomizer)
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
).filter((type) => featureFlags.preschoolEnabled || type !== 'PRESCHOOL')
