// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import customizations from '@evaka/customizations/employee'
import type { EmployeeCustomizations } from './types'
import { fi } from './espoo/employee/assets/i18n/fi'
import { isArray, mergeWith } from 'lodash'
import { ApplicationType } from 'lib-common/api-types/application/enums'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const {
  appConfig,
  cityLogo,
  featureFlags,
  assistanceMeasures,
  placementTypes,
  placementPlanRejectReasons,
  unitProviderTypes
}: EmployeeCustomizations = customizations
export {
  appConfig,
  cityLogo,
  featureFlags,
  assistanceMeasures,
  placementTypes,
  placementPlanRejectReasons,
  unitProviderTypes
}

export type Lang = 'fi'

export type Translations = typeof fi

const customizer = <T extends any>( // eslint-disable-line @typescript-eslint/no-explicit-any
  origValue: T,
  customizedValue: T | undefined
): T | undefined => {
  if (isArray(origValue) && customizedValue != undefined) {
    return customizedValue
  }
  return undefined
}

export const translations: { [K in Lang]: Translations } = {
  fi: mergeWith(
    fi,
    (customizations as EmployeeCustomizations).translations.fi,
    customizer
  )
}

export const applicationTypes: ApplicationType[] = (
  ['DAYCARE', 'PRESCHOOL', 'CLUB'] as const
).filter((type) => featureFlags.preschoolEnabled || type !== 'PRESCHOOL')
