// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { GuardianInfo } from 'lib-common/generated/api-types/application'
import type {
  DecisionDraft,
  DecisionUnit
} from 'lib-common/generated/api-types/decision'

export interface DecisionDraftGroup {
  decisions: DecisionDraft[]
  placementUnitName: string
  unit: DecisionUnit
  guardian: GuardianInfo
  otherGuardian: GuardianInfo | null
  child: GuardianInfo
}
