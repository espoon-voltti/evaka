// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { LatLngExpression } from 'leaflet'
import { Theme } from 'lib-common/theme'
import {
  Lang as LangCitizen,
  Translations as TranslationsCitizen
} from './citizen'
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

export type AssistanceMeasure =
  | 'SPECIAL_ASSISTANCE_DECISION'
  | 'INTENSIFIED_ASSISTANCE'
  | 'EXTENDED_COMPULSORY_EDUCATION'
  | 'CHILD_SERVICE'
  | 'CHILD_ACCULTURATION_SUPPORT'
  | 'TRANSPORT_BENEFIT'

export interface CommonCustomizations {
  theme: Theme
}

export interface CitizenCustomizations {
  translations: Record<LangCitizen, DeepPartial<TranslationsCitizen>>
  cityLogo: {
    src: string
    alt: string
  }
  footerLogo?: {
    src: string
    alt: string
  }
  routeLinkRootUrl: string
  mapConfig: MapConfig
  featureFlags: FeatureFlags
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
  daycareApplicationServiceNeedOptionsEnabled: boolean
  urgencyAttachmentsEnabled: boolean
  preschoolEnabled: boolean
  assistanceActionOtherEnabled: boolean
}

export interface EmployeeCustomizations {
  translations: Record<LangEmployee, DeepPartial<TranslationsEmployee>>
  cityLogo: {
    src: string
    alt: string
  }
  featureFlags: FeatureFlags
  assistanceMeasures: AssistanceMeasure[]
}
