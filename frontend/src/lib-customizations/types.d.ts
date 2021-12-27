// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { LatLngExpression } from 'leaflet'
import { Theme } from 'lib-common/theme'
import { DeepReadonly } from 'lib-common/types'
import {
  Lang as LangCitizen,
  Translations as TranslationsCitizen
} from './citizen'
import {
  Lang as LangEmployee,
  Translations as TranslationsEmployee,
  VasuLang as VasuLangEmployee,
  VasuTranslations as VasuTranslationsEmployee
} from './employee'
import {
  Lang as LangEmployeeMobile,
  Translations as TranslationsEmployeeMobile
} from './employeeMobile'
import { PlacementType } from 'lib-common/generated/enums'

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

export type PlacementPlanRejectReason =
  | 'OTHER'
  | 'REASON_1'
  | 'REASON_2'
  | 'REASON_3'

export type UnitProviderType =
  | 'MUNICIPAL'
  | 'PURCHASED'
  | 'PRIVATE'
  | 'MUNICIPAL_SCHOOL'
  | 'PRIVATE_SERVICE_VOUCHER'
  | 'EXTERNAL_PURCHASED'

export interface BaseAppConfig {
  sentry?: {
    dsn: string
    enabled: boolean
  }
}

export interface CommonCustomizations {
  theme: Theme
}

export interface CitizenCustomizations {
  appConfig: BaseAppConfig
  langs: LangCitizen[]
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

/**
 * Frontend features to enable.
 *
 * See lib-customizations/espoo/featureFlags.tsx for an example of configuring
 * feature flags separately per environment with shared defaults.
 */
interface BaseFeatureFlags {
  assistanceActionOtherEnabled: boolean
  childIncomeEnabled: boolean
  daycareApplication: {
    dailyTimesEnabled: boolean
  }
  groupsTableServiceNeedsEnabled: boolean
  evakaLogin: boolean
  financeBasicsPage: boolean
  preschoolEnabled: boolean
  urgencyAttachmentsEnabled: boolean
  adminSettingsEnabled: boolean

  /**
   * Experimental flags are features in development: features that aren't yet
   * recommended/tested for production usage but can be enabled for testing
   * in eVaka implementations. These flags will either be dropped when features
   * are deemed ready or promoted to top-level flags.
   */
  experimental?: {
    ai?: boolean
    messageAttachments?: boolean
    realtimeStaffAttendance?: boolean
    personalDetailsPage?: boolean
    mobileMessages?: boolean
    leops?: boolean
    placementTermination?: boolean
  }
}

export type FeatureFlags = DeepReadonly<BaseFeatureFlags>

export interface EmployeeCustomizations {
  appConfig: BaseAppConfig
  translations: Record<LangEmployee, DeepPartial<TranslationsEmployee>>
  vasuTranslations: Record<
    VasuLangEmployee,
    DeepPartial<VasuTranslationsEmployee>
  >
  cityLogo: {
    src: string
    alt: string
  }
  featureFlags: FeatureFlags
  placementTypes: PlacementType[]
  assistanceMeasures: AssistanceMeasure[]
  placementPlanRejectReasons: PlacementPlanRejectReason[]
  unitProviderTypes: UnitProviderType[]
}

export interface EmployeeMobileCustomizations {
  appConfig: BaseAppConfig
  translations: Record<
    LangEmployeeMobile,
    DeepPartial<TranslationsEmployeeMobile>
  >
}
