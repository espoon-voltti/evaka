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
import { fi } from './defaults/employee/i18n/fi'
import { sv } from './defaults/employee/i18n/sv'
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

const {
  appConfig,
  cityLogo,
  featureFlags,
  absenceTypes,
  daycareAssistanceLevels,
  otherAssistanceMeasureTypes,
  preschoolAssistanceLevels,
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

export const applicationTypes: ApplicationType[] = (
  ['DAYCARE', 'PRESCHOOL', 'CLUB'] as const
).filter((type) => featureFlags.preschool || type !== 'PRESCHOOL')
