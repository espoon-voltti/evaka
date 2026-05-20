// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { FeatureFlags } from 'lib-customizations/types'

import type { Env } from './env'
import { env } from './env'

type Features = {
  default: FeatureFlags
} & Record<Env, FeatureFlags>

const prod: FeatureFlags = {
  environmentLabel: null,
  citizenShiftCareAbsence: true,
  assistanceActionOther: true,
  daycareApplication: {
    dailyTimes: true,
    serviceNeedOption: false
  },
  preschoolApplication: {
    connectedDaycarePreferredStartDate: true,
    serviceNeedOption: false
  },
  decisionDraftMultipleUnits: false,
  preschool: true,
  preparatory: true,
  urgencyAttachments: true,
  financeDecisionHandlerSelect: false,
  feeDecisionPreschoolClubFilter: false,
  placementGuarantee: true,
  extendedPreschoolTerm: true,
  citizenAttendanceSummary: false,
  voucherUnitPayments: false,
  voucherValueSeparation: true,
  intermittentShiftCare: false,
  noAbsenceType: false,
  discussionReservations: true,
  forceUnpublishDocumentTemplate: false,
  invoiceDisplayAccountNumber: true,
  serviceApplications: false,
  multiSelectDeparture: true,
  archiveIntegration: {
    childDocuments: true
  },
  aromiIntegration: true,
  citizenChildDocumentTypes: true,
  decisionChildDocumentTypes: true,
  showCitizenApplicationPreschoolTerms: false,
  missingQuestionnaireAnswerMarkerEnabled: false,
  absenceApplications: true,
  showMetadataToCitizen: true,
  placementDesktop: true,
  employeeLanguageSelection: true
}

const features: Features = {
  default: {
    ...prod,
    environmentLabel: 'Lokaali',
    voucherUnitPayments: true,
    jamixIntegration: true,
    nekkuIntegration: false,
    forceUnpublishDocumentTemplate: true,
    serviceApplications: true,
    showCitizenApplicationPreschoolTerms: true,
    decisionReasoningOptions: true
  },
  staging: {
    ...prod,
    environmentLabel: 'Staging',
    jamixIntegration: false,
    nekkuIntegration: false,
    forceUnpublishDocumentTemplate: true,
    serviceApplications: true,
    showCitizenApplicationPreschoolTerms: true,
    decisionReasoningOptions: true
  },
  prod
}

const featureFlags = features[env()]

export default featureFlags
