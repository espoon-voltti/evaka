// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

declare module '@evaka/customizations/citizen' {
  import React from 'react'

  import { ApplicationType } from 'lib-common/generated/api-types/application'
  import { Lang, Translations } from 'lib-customizations/citizen'
  import {
    BaseAppConfig,
    DeepPartial,
    FeatureFlags,
    ImgProps,
    MapConfig
  } from 'lib-customizations/types'

  export const appConfig: BaseAppConfig
  export const langs: readonly Lang[]
  export const translations: Record<Lang, DeepPartial<Translations>>
  export const cityLogo: ImgProps
  export const footerLogo: React.ReactNode
  export const routeLinkRootUrl: string
  export const mapConfig: MapConfig
  export const featureFlags: FeatureFlags
  export const getMaxPreferredUnits: (type: ApplicationType) => number
}
