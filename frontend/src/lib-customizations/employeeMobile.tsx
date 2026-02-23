// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import defaultsUntyped from '@evaka/customizations/employeeMobile'
import mergeWith from 'lodash/mergeWith'

import type { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import type { JsonOf } from 'lib-common/json'

import { mergeCustomizer } from './common'
import { fi } from './defaults/employee-mobile-frontend/i18n/fi'
import { sv } from './defaults/employee-mobile-frontend/i18n/sv'
import type { EmployeeMobileCustomizations } from './types'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const defaults: EmployeeMobileCustomizations = defaultsUntyped

declare global {
  interface EvakaWindowConfig {
    employeeMobileCustomizations?: Partial<JsonOf<EmployeeMobileCustomizations>>
  }
}

const overrides =
  typeof window !== 'undefined'
    ? window.evaka?.employeeMobileCustomizations
    : undefined

const customizations: EmployeeMobileCustomizations = overrides
  ? mergeWith({}, defaults, overrides, mergeCustomizer)
  : defaults

const {
  appConfig,
  featureFlags,
  additionalStaffAttendanceTypes
}: EmployeeMobileCustomizations = customizations
const isStaffAttendanceTypesEnabled = additionalStaffAttendanceTypes.length > 0
const staffAttendanceTypes: StaffAttendanceType[] = [
  'PRESENT',
  ...additionalStaffAttendanceTypes
]
export {
  appConfig,
  featureFlags,
  isStaffAttendanceTypesEnabled,
  staffAttendanceTypes
}

export type Lang = 'fi' | 'sv'
export type Translations = typeof fi
export const langs: Lang[] = ['fi', 'sv']

export const translations: Record<Lang, Translations> = {
  fi: mergeWith({}, fi, customizations.translations.fi, mergeCustomizer),
  sv: mergeWith({}, sv, customizations.translations.sv, mergeCustomizer)
}
