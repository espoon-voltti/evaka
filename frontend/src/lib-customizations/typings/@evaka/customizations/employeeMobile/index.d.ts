// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

declare module '@evaka/customizations/employeeMobile' {
  import type { BrowserOptions as SentryOptions } from '@sentry/browser'

  import { Lang, Translations } from 'lib-customizations/employeeMobile'
  import { FeatureFlags } from 'lib-customizations/types'

  export const sentryConfig: SentryOptions | undefined
  export const featureFlags: FeatureFlags
  export const translations: Record<Lang, Translations>
}
