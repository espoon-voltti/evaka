// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { DeepReadonly } from 'lib-common/types'

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
  archiveIntegration?: {
    decisions?: boolean
    childDocuments?: boolean
    feeDecisions?: boolean
    voucherValueDecisions?: boolean
  }

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

  /**
   * Enable option to display applications in a service worker placement desktop mode
   */
  placementDesktop?: boolean

  /**
   * Enable language selection in employee frontends
   */
  employeeLanguageSelection?: boolean
}

export type FeatureFlags = DeepReadonly<BaseFeatureFlags>
