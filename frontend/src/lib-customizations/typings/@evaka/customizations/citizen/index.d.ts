// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

declare module '@evaka/customizations/citizen' {
  import type { BrowserOptions as SentryOptions } from '@sentry/browser'
  import React from 'react'

  import { ApplicationType } from 'lib-common/generated/api-types/application'
  import { Lang, Translations } from 'lib-customizations/citizen'
  import { FeatureFlags, ImgProps, MapConfig } from 'lib-customizations/types'

  export const sentryConfig: SentryOptions | undefined
  export const langs: Lang[]
  export const translations: Record<Lang, Translations>
  export const cityLogo: ImgProps
  export const footerLogo: React.ReactNode
  export const routeLinkRootUrl: string
  export const mapConfig: MapConfig
  export const featureFlags: FeatureFlags
  export const getMaxPreferredUnits: (type: ApplicationType) => number
}
