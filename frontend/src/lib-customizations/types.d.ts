// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { LatLngExpression } from 'leaflet'
import type React from 'react'

import type { FeatureFlags } from 'lib-common/feature-flags'
import type { AbsenceType } from 'lib-common/generated/api-types/absence'
import type { ApplicationType } from 'lib-common/generated/api-types/application'
import type {
  DaycareAssistanceLevel,
  OtherAssistanceMeasureType,
  PreschoolAssistanceLevel
} from 'lib-common/generated/api-types/assistance'
import type { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import type { ProviderType } from 'lib-common/generated/api-types/daycare'
import type { VoucherValueDecisionType } from 'lib-common/generated/api-types/invoicing'
import type {
  PlacementPlanRejectReason,
  PlacementType
} from 'lib-common/generated/api-types/placement'
import type LocalDate from 'lib-common/local-date'
import type { Theme } from 'lib-common/theme'

import type {
  Lang as LangCitizen,
  Translations as TranslationsCitizen
} from './citizen'
import type {
  Lang as LangEmployee,
  Translations as TranslationsEmployee
} from './employee'
import type {
  Lang as LangEmployeeMobile,
  Translations as TranslationsEmployeeMobile
} from './employeeMobile'

declare global {
  interface Window {
    evaka?: EvakaWindowConfig
  }

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
  footerLogo?: React.JSX.Element
  routeLinkRootUrl?: string
  mapConfig: MapConfig
  featureFlags: FeatureFlags
  getMaxPreferredUnits: (type: ApplicationType) => number
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
  careTypeFilters: ApplicationType[]
  unitProviderTypeFilters: ProviderType[]
}

type CityLogo = React.JSX.Element | ImgProps

type CustomizableStaffAttendanceType = Exclude<StaffAttendanceType, 'PRESENT'>

export interface EmployeeCustomizations {
  appConfig: BaseAppConfig
  translations: Record<LangEmployee, DeepPartial<TranslationsEmployee>>
  cityLogo: CityLogo
  featureFlags: FeatureFlags
  placementTypes: PlacementType[]
  absenceTypes: AbsenceType[]
  absenceTypesNotSelectableInWeekCalendar: AbsenceType[]
  daycareAssistanceLevels: DaycareAssistanceLevel[]
  otherAssistanceMeasureTypes: OtherAssistanceMeasureType[]
  placementPlanRejectReasons: PlacementPlanRejectReason[]
  preschoolAssistanceLevels: PreschoolAssistanceLevel[]
  unitProviderTypes: ProviderType[]
  voucherValueDecisionTypes: VoucherValueDecisionType[]
  additionalStaffAttendanceTypes: CustomizableStaffAttendanceType[]
  additionalPlacementTypesForDocumentTemplates?: PlacementType[]
  getPaymentsDueDate?: () => LocalDate
}

export interface EmployeeMobileCustomizations {
  appConfig: BaseAppConfig
  featureFlags: FeatureFlags
  translations: Record<
    LangEmployeeMobile,
    DeepPartial<TranslationsEmployeeMobile>
  >
  additionalStaffAttendanceTypes: CustomizableStaffAttendanceType[]
}
