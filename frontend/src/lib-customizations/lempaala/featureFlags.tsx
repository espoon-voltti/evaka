// SPDX-FileCopyrightText: 2024 Tampere region
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
  citizenShiftCareAbsence: false,
  daycareApplication: {
    dailyTimes: false,
    serviceNeedOption: true
  },
  preschoolApplication: {
    connectedDaycarePreferredStartDate: true,
    serviceNeedOption: true
  },
  decisionDraftMultipleUnits: true,
  urgencyAttachments: true,
  preschool: false,
  preparatory: false,
  assistanceActionOther: false,
  financeDecisionHandlerSelect: true,
  feeDecisionPreschoolClubFilter: true,
  placementGuarantee: true,
  intermittentShiftCare: true,
  citizenAttendanceSummary: true,
  noAbsenceType: true,
  voucherUnitPayments: false,
  voucherValueSeparation: false,
  extendedPreschoolTerm: false,
  hideClubApplication: true,
  discussionReservations: true,
  jamixIntegration: true,
  serviceApplications: true,
  absenceApplications: false,
  multiSelectDeparture: true,
  requireAttachments: true,
  showCitizenApplicationPreschoolTerms: true,
  decisionChildDocumentTypes: true,
  placementDesktop: true
}

const features: Features = {
  default: {
    ...prod,
    environmentLabel: 'Test',
    citizenChildDocumentTypes: true
  },
  prod
}

const featureFlags = features[env()]

export default featureFlags
