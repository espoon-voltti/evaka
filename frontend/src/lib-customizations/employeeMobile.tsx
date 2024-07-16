// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import defaultsUntyped from '@evaka/customizations/employeeMobile'
import mergeWith from 'lodash/mergeWith'

import { JsonOf } from 'lib-common/json'

import { mergeCustomizer } from './common'
import { fi } from './defaults/employee-mobile-frontend/i18n/fi'
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

const { appConfig, featureFlags }: EmployeeMobileCustomizations = customizations
export { appConfig, featureFlags }

export type Lang = 'fi'
export type Translations = typeof fi

export const translations: { [K in Lang]: Translations } = {
  fi: mergeWith({}, fi, customizations.translations.fi, mergeCustomizer)
}
