// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { featureFlags as baseFeatureFlags } from '@evaka/customizations/employee'
import mergeWith from 'lodash/mergeWith'

import { ApplicationType } from 'lib-common/generated/api-types/application'

import { mergeCustomizer } from './common'
import { fi } from './defaults/employee/i18n/fi'
import { fi as vasuFI } from './defaults/employee/i18n/vasu/fi'
import { FeatureFlags } from './types'

export {
  sentryConfig,
  cityLogo,
  absenceTypes,
  assistanceMeasures,
  daycareAssistanceLevels,
  otherAssistanceMeasureTypes,
  preschoolAssistanceLevels,
  placementTypes,
  placementPlanRejectReasons,
  unitProviderTypes,
  voucherValueDecisionTypes,
  translations,
  vasuTranslations
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

export type VasuLang = 'FI' | 'SV'
export type VasuTranslations = typeof vasuFI

export const applicationTypes: ApplicationType[] = (
  ['DAYCARE', 'PRESCHOOL', 'CLUB'] as const
).filter((type) => featureFlags.preschool || type !== 'PRESCHOOL')
