// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import customizations from '@evaka/customizations/employee'
import type { EmployeeCustomizations } from './types'
import { fi } from './espoo/employee/assets/i18n/fi'
import { merge } from 'lodash'
import { ApplicationType } from 'lib-common/api-types/application/enums'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const {
  cityLogo,
  featureFlags,
  assistanceMeasures,
  placementTypes,
  placementPlanRejectReasons
}: EmployeeCustomizations = customizations
export {
  cityLogo,
  featureFlags,
  assistanceMeasures,
  placementTypes,
  placementPlanRejectReasons
}

export type Lang = 'fi'

export type Translations = typeof fi

export const translations: { [K in Lang]: Translations } = {
  fi: merge(fi, (customizations as EmployeeCustomizations).translations.fi)
}

export const applicationTypes: ApplicationType[] = ([
  'DAYCARE',
  'PRESCHOOL',
  'CLUB'
] as const).filter(
  (type) => featureFlags.preschoolEnabled || type !== 'PRESCHOOL'
)
