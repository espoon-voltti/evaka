// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import customizations from '@evaka/customizations/employeeMobile'
import { mergeWith } from 'lodash'

import { translationsMergeCustomizer } from './common'
import { fi } from './espoo/employee-mobile-frontend/assets/i18n/fi'
import type { EmployeeMobileCustomizations } from './types'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const { appConfig }: EmployeeMobileCustomizations = customizations
export { appConfig }

export type Lang = 'fi'
export type Translations = typeof fi

export const translations: { [K in Lang]: Translations } = {
  fi: mergeWith(
    fi,
    (customizations as EmployeeMobileCustomizations).translations.fi,
    translationsMergeCustomizer
  )
}
