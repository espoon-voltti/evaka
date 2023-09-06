// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'react'

import type * as CitizenAlias from '@evaka/customizations/citizen'
import type * as CommonAlias from '@evaka/customizations/common'
import type * as EmployeeAlias from '@evaka/customizations/employee'
import type * as EmployeeMobileAlias from '@evaka/customizations/employeeMobile'
import type { LatLngExpression } from 'leaflet'

import { ApplicationType } from 'lib-common/generated/api-types/application'
import { ProviderType } from 'lib-common/generated/api-types/daycare'
import { JsonOf } from 'lib-common/json'
import { DeepReadonly } from 'lib-common/types'

export type CitizenModule = typeof CitizenAlias
export type CommonModule = typeof CommonAlias
export type EmployeeMobileModule = typeof EmployeeMobileAlias
export type EmployeeModule = typeof EmployeeAlias

declare global {
  interface Window {
    evaka?: EvakaWindowConfig
  }

  interface EvakaWindowConfig {
    overrides?: {
      featureFlags?: Partial<JsonOf<FeatureFlags>>
      citizen?: {
        langs?: JsonOf<CitizenModule['langs']>
      }
    }
  }
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

export interface MapConfig {
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
   * Whether to show PLANNED_ABSENCE as a third absence option for shift care children in
   * citizen's absence modal
   */
  citizenShiftCareAbsence: boolean
  /**
   * Whether to show PLANNED_ABSENCE as a third absence option for contract day children in
   * citizen's absence modal
   */
  citizenContractDayAbsence: boolean

  /**
   * Enable assistance action type "other"
   */
  assistanceActionOther: boolean

  daycareApplication: {
    /**
     * Citizen must specify daily daycare start and end times the daycare application
     */
    dailyTimes: boolean
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
   * Enable section for child discussion related data (Lapset puheeksi) in child documents section
   */
  childDiscussion: boolean

  /**
   * Enable support for filtering fee decisions by preschool club placement type
   */
  feeDecisionPreschoolClubFilter: boolean

  /**
   * Experimental flags are features in development: features that aren't yet
   * recommended/tested for production usage but can be enabled for testing
   * in eVaka implementations. These flags will either be dropped when features
   * are deemed ready or promoted to top-level flags.
   */
  experimental?: {
    /**
     * Enable support for LEOPS (lapsen esiopetussuunnitelma)
     */
    leops?: boolean

    /**
     * Enable payments for voucher units (palvelusetelimaksatus)
     */
    voucherUnitPayments?: boolean

    /**
     * Enable assistance need decisions (tuen päätös)
     */
    assistanceNeedDecisions?: boolean

    /**
     * Enable assistance need preschool decisions (esiopetuksen tuen päätös)
     */
    assistanceNeedPreschoolDecisions?: boolean

    /**
     * Enable language selection for assistance need decisions
     */
    assistanceNeedDecisionsLanguageSelect?: boolean

    /**
     * Enable attendance types for realtime staff attendances, instead of just present/absent
     */
    staffAttendanceTypes?: boolean

    /**
     * Enable support for foster parents
     */
    fosterParents?: boolean

    /**
     * Enable support for messaging to application guardian for service workers (palveluohjauksen viestintä)
     */
    serviceWorkerMessaging?: boolean

    /**
     * Enable support for duplicating a child as a new SSN-less person
     */
    personDuplicate?: boolean

    /**
     * Enable support for new template editor and child documents
     */
    childDocuments?: boolean

    /**
     * Enable support for intermittent shift care
     */
    intermittentShiftCare?: boolean

    /**
     * Show email notification settings in citizen's personal details page
     */
    citizenEmailNotificationSettings?: boolean

    /**
     * Show attendance summary for contract days children
     */
    citizenAttendanceSummary?: boolean

    /**
     * Allows marking fee decision drafts as ignored
     */
    feeDecisionIgnoredStatus?: boolean
  }
}

export type FeatureFlags = DeepReadonly<BaseFeatureFlags>
