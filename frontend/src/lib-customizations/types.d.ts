// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'react'
import type { LatLngExpression } from 'leaflet'

import { AssistanceMeasure } from 'lib-common/generated/api-types/assistanceaction'
import {
  AbsenceType,
  ProviderType
} from 'lib-common/generated/api-types/daycare'
import { VoucherValueDecisionType } from 'lib-common/generated/api-types/invoicing'
import {
  PlacementPlanRejectReason,
  PlacementType
} from 'lib-common/generated/api-types/placement'
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

declare global {
  interface Window {
    evaka?: EvakaWindowConfig
  }

  // eslint-disable-next-line @typescript-eslint/no-empty-interface
  interface EvakaWindowConfig {}
}

type DeepPartial<T> = {
  [P in keyof T]?: T[P] extends (infer U)[]
    ? DeepPartial<U>[]
    : T[P] extends Readonly<infer U>[]
    ? Readonly<DeepPartial<U>>[]
    : DeepPartial<T[P]>
}

interface ImgProps {
  src: string
  alt: string
}

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
  cityLogo: ImgProps
  footerLogo?: JSX.Element
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
  /**
   * Whether to show PLANNED_ABSENCE as a third absence option for shift care children in
   * citizen's absence modal
   */
  citizenShiftCareAbsenceEnabled: boolean

  assistanceActionOtherEnabled: boolean
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
    personalDetailsPage?: boolean
    mobileMessages?: boolean
    leops?: boolean
    citizenVasu?: boolean
    voucherUnitPayments?: boolean
    assistanceNeedDecisions?: boolean
    assistanceNeedDecisionsLanguageSelect?: boolean
    assistanceNeedVoucherCoefficients?: boolean
    staffAttendanceTypes?: boolean
  }
}

export type FeatureFlags = DeepReadonly<BaseFeatureFlags>

type CityLogo = JSX.Element | ImgProps

export interface EmployeeCustomizations {
  appConfig: BaseAppConfig
  translations: Record<LangEmployee, DeepPartial<TranslationsEmployee>>
  vasuTranslations: Record<
    VasuLangEmployee,
    DeepPartial<VasuTranslationsEmployee>
  >
  cityLogo: CityLogo
  featureFlags: FeatureFlags
  placementTypes: PlacementType[]
  absenceTypes: AbsenceType[]
  assistanceMeasures: AssistanceMeasure[]
  placementPlanRejectReasons: PlacementPlanRejectReason[]
  unitProviderTypes: ProviderType[]
  voucherValueDecisionTypes: VoucherValueDecisionType[]
}

export interface EmployeeMobileCustomizations {
  appConfig: BaseAppConfig
  featureFlags: FeatureFlags
  translations: Record<
    LangEmployeeMobile,
    DeepPartial<TranslationsEmployeeMobile>
  >
}
