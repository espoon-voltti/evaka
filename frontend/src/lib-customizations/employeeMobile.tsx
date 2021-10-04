// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import customizations from '@evaka/customizations/employeeMobile'
import type { EmployeeMobileCustomizations } from './types'
import { fi } from './espoo/employee-mobile-frontend/assets/i18n/fi'
import { mergeWith } from 'lodash'
import { translationsMergeCustomizer } from './common'

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
