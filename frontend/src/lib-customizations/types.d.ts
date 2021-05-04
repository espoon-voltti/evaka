// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { LatLngExpression } from 'leaflet'
import {
  Lang as LangEmployee,
  Translations as TranslationsEmployee
} from './employee'

type DeepPartial<T> = {
  [P in keyof T]?: T[P] extends (infer U)[]
    ? DeepPartial<U>[]
    : T[P] extends Readonly<infer U>[]
    ? Readonly<DeepPartial<U>>[]
    : DeepPartial<T[P]>
}

export interface CitizenCustomizations {
  fiCustomizations: CitizenLocalizations
  enCustomizations: CitizenLocalizations
  svCustomizations: CitizenLocalizations
  cityLogo: {
    src: string
    alt: string
  }
  mapConfig: MapConfig
  featureFlags: FeatureFlags
}

interface CitizenLocalizations {
  applicationsList: {
    title: string
    summary: string
  }
  footer: {
    cityLabel: string
    privacyPolicyLink: string
    sendFeedbackLink: string
  }
}

interface MapConfig {
  center: LatLngExpression
  initialZoom: number
  addressZoom: number
  searchAreaRect: {
    minLongitude: number
    maxLongitude: number
    minLatitude: number
    maxLatitude: number
  }
}

interface FeatureFlags {
  urgencyAttachmentsEnabled: boolean
}

export interface EmployeeCustomizations {
  translations: Record<LangEmployee, DeepPartial<TranslationsEmployee>>
  cityLogo: {
    src: string
    alt: string
  }
  featureFlags: FeatureFlags
}
