// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

declare module '@evaka/customizations/employeeMobile' {
  import { Lang, Translations } from 'lib-customizations/employeeMobile'
  import { BaseAppConfig, FeatureFlags } from 'lib-customizations/types'

  export const appConfig: BaseAppConfig
  export const featureFlags: FeatureFlags
  export const translations: Record<Lang, Translations>
}
