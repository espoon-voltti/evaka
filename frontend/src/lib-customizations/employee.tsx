// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import customizations from '@evaka/customizations/employee'
import { mergeWith } from 'lodash'
import { ApplicationType } from 'lib-common/generated/api-types/application'
import { translationsMergeCustomizer } from './common'
import { fi } from './espoo/employee/assets/i18n/fi'
import { fi as vasuFI } from './espoo/employee/assets/i18n/vasu/fi'
import { sv as vasuSV } from './espoo/employee/assets/i18n/vasu/sv'
import type { EmployeeCustomizations } from './types'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const {
  appConfig,
  cityLogo,
  featureFlags,
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
  assistanceMeasures,
  placementTypes,
  placementPlanRejectReasons,
  unitProviderTypes,
  voucherValueDecisionTypes
}

export type Lang = 'fi'

export type Translations = typeof fi

export const translations: { [K in Lang]: Translations } = {
  fi: mergeWith(
    fi,
    (customizations as EmployeeCustomizations).translations.fi,
    translationsMergeCustomizer
  )
}

export type VasuLang = 'FI' | 'SV'
export type VasuTranslations = typeof vasuFI

export const vasuTranslations: { [K in VasuLang]: VasuTranslations } = {
  FI: mergeWith(
    vasuFI,
    (customizations as EmployeeCustomizations).vasuTranslations.FI,
    translationsMergeCustomizer
  ),
  SV: mergeWith(
    vasuSV,
    (customizations as EmployeeCustomizations).vasuTranslations.SV,
    translationsMergeCustomizer
  )
}

export const applicationTypes: ApplicationType[] = (
  ['DAYCARE', 'PRESCHOOL', 'CLUB'] as const
).filter((type) => featureFlags.preschoolEnabled || type !== 'PRESCHOOL')
