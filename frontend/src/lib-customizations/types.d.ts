// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { LatLngExpression } from 'leaflet'
import type React from 'react'

import type { AbsenceType } from 'lib-common/generated/api-types/absence'
import type { ApplicationType } from 'lib-common/generated/api-types/application'
import type {
  DaycareAssistanceLevel,
  OtherAssistanceMeasureType,
  PreschoolAssistanceLevel
} from 'lib-common/generated/api-types/assistance'
import type { ProviderType } from 'lib-common/generated/api-types/daycare'
import type { VoucherValueDecisionType } from 'lib-common/generated/api-types/invoicing'
import type {
  PlacementPlanRejectReason,
  PlacementType
} from 'lib-common/generated/api-types/placement'
import type LocalDate from 'lib-common/local-date'
import type { Theme } from 'lib-common/theme'
import type { DeepReadonly } from 'lib-common/types'

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

/**
 * Frontend features to enable.
 *
 * See lib-customizations/espoo/featureFlags.tsx for an example of configuring
 * feature flags separately per environment with shared defaults.
 */
interface BaseFeatureFlags {
  /**
   * Name of the environment to be displayed on the upper left corner.
   * Use null for prod environment to not display it.
   */
  environmentLabel: string | null

  /**
   * Whether to show PLANNED_ABSENCE as a third absence option for shift care children in
   * citizen's absence modal
   */
  citizenShiftCareAbsence: boolean

  /**
   * Enable assistance action type "other"
   */
  assistanceActionOther: boolean

  daycareApplication: {
    /**
     * Citizen must specify daily daycare start and end times the daycare application
     */
    dailyTimes: boolean
    /**
     * Citizen must select a service need option on daycare application
     */
    serviceNeedOption: boolean
  }
  preschoolApplication: {
    /**
     * Citizen must select the preferred start date for connected daycare on preschool application
     */
    connectedDaycarePreferredStartDate: boolean
    /**
     * Citizen must select a service need option on preschool application
     */
    serviceNeedOption: boolean
  }

  /**
   * Separate units can be selected for each decision on a decision draft (sijoitushahmotelma)
   *
   * Preschool + connected daycare applications generate two decisions. This flag enables selecting
   * different units for each decision.
   */
  decisionDraftMultipleUnits: boolean

  /**
   * Enable support for preschool
   */
  preschool: boolean

  /**
   * Enable support for preparatory education
   */
  preparatory: boolean

  /**
   * Require one or more attachments for urgent applications
   */
  urgencyAttachments: boolean

  /**
   * Enable support for selecting finance decision handler when sending the decision
   */
  financeDecisionHandlerSelect: boolean

  /**
   * Enable support for filtering fee decisions by preschool club placement type
   */
  feeDecisionPreschoolClubFilter: boolean

  /**
   * Enable placement guarantee selection
   */
  placementGuarantee: boolean

  /**
   * Enable support for intermittent shift care
   */
  intermittentShiftCare: boolean

  /**
   * Show attendance summary for contract days children
   */
  citizenAttendanceSummary: boolean

  /**
   * Enables no absence type in mobile
   */
  noAbsenceType: boolean

  /**
   * Enable payments for voucher units (palvelusetelimaksatus)
   */
  voucherUnitPayments: boolean

  /**
   * Whether to show voucher value sums before assistance need factor and the effect of
   * assistance need factor as separate columns in voucher value report.
   */
  voucherValueSeparation: boolean

  /**
   * Enable language selection for assistance need decisions
   */
  assistanceNeedDecisionsLanguageSelect: boolean

  /**
   * Enable attendance types for realtime staff attendances, instead of just present/absent
   */
  staffAttendanceTypes: boolean

  /**
   * Enable extended period start date -field when creating or editing preschool terms
   */
  extendedPreschoolTerm: boolean

  /**
   * Experimental flags are features in development: features that aren't yet
   * recommended/tested for production usage but can be enabled for testing
   * in eVaka implementations. They are optional (have `?` after the property name)
   * so that they can be enabled without breaking the build for other environments.
   *
   * These flags will either be dropped when features are deemed ready or promoted
   * to top-level flags (moved up, `?` removed).
   */

  /**
   * Hide option to create a club application in citizen UI
   */
  hideClubApplication?: boolean

  /**
   * EXPERIMENTAL: Enable discussion reservation surveys
   */
  discussionReservations?: boolean

  /**
   * Display Jamix food ordering service related functions
   */
  jamixIntegration?: boolean

  /**
   * Display Aromi food ordering service related functions
   */
  aromiIntegration?: boolean

  /**
   * Display Nekku food ordering service related functions
   */
  nekkuIntegration?: boolean

  /**
   * Allow admin to force unpublish document templates.
   * Do not set in production.
   * Note the corresponding backend environment variable feature flag.
   */
  forceUnpublishDocumentTemplate?: boolean

  /**
   * Display account number on invoice details view.
   */
  invoiceDisplayAccountNumber?: boolean

  /**
   * Enable a feature where a citizen can apply for a change in service need.
   */
  serviceApplications?: boolean

  /**
   * Enable a feature where a citizen can create absence applications.
   */
  absenceApplications?: boolean

  /**
   * Show the Titania errors report in the reports list
   */
  titaniaErrorsReport?: boolean

  /**
   * Allow marking multiple children as departed in the employee mobile
   */
  multiSelectDeparture?: boolean

  /**
   * Missing attachments as an error
   */
  requireAttachments?: boolean

  /**
   * Enable support for document archival integration
   */
  archiveIntegrationEnabled?: boolean

  /**
   * Enable support for citizen child document types
   */
  citizenChildDocumentTypes?: boolean

  /**
   * Enable support for decision child document types
   */
  decisionChildDocumentTypes?: boolean

  /**
   * Enable showing preschool extended term data for citizen preschool application
   */
  showCitizenApplicationPreschoolTerms?: boolean

  /**
   * Enable missing holiday questionnaire answer indicator
   */
  missingQuestionnaireAnswerMarkerEnabled?: boolean

  /**
   * Enable showing metadata in citizen applications
   */
  showMetadataToCitizen?: boolean
}

export type FeatureFlags = DeepReadonly<BaseFeatureFlags>

type CityLogo = React.JSX.Element | ImgProps

export interface EmployeeCustomizations {
  appConfig: BaseAppConfig
  translations: Record<LangEmployee, DeepPartial<TranslationsEmployee>>
  cityLogo: CityLogo
  featureFlags: FeatureFlags
  placementTypes: PlacementType[]
  absenceTypes: AbsenceType[]
  daycareAssistanceLevels: DaycareAssistanceLevel[]
  otherAssistanceMeasureTypes: OtherAssistanceMeasureType[]
  placementPlanRejectReasons: PlacementPlanRejectReason[]
  preschoolAssistanceLevels: PreschoolAssistanceLevel[]
  unitProviderTypes: ProviderType[]
  voucherValueDecisionTypes: VoucherValueDecisionType[]
  getPaymentsDueDate?: () => LocalDate
}

export interface EmployeeMobileCustomizations {
  appConfig: BaseAppConfig
  featureFlags: FeatureFlags
  translations: Record<
    LangEmployeeMobile,
    DeepPartial<TranslationsEmployeeMobile>
  >
}
