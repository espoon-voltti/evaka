// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import customizations from '@evaka/customizations/employee'
import type { EmployeeCustomizations } from './types'
import { fi } from './espoo/employee/assets/i18n/fi'
import deepmerge from 'deepmerge'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const { cityLogo, featureFlags }: EmployeeCustomizations = customizations
export { cityLogo, featureFlags }

export type Lang = 'fi'

export type Translations = typeof fi

export const translations: {[K in Lang]: Translations} = {
  // eslint-disable-next-line @typescript-eslint/ban-ts-comment
  // @ts-ignore
  fi: deepmerge(fi, (customizations as EmployeeCustomizations).translations.fi)
}
