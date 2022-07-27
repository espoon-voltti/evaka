// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ApplicationDecisions } from 'lib-common/generated/api-types/application'
import type {
  DecisionStatus,
  DecisionType,
  DecisionUnit
} from 'lib-common/generated/api-types/decision'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

export function deserializeApplicationDecisions(
  json: JsonOf<ApplicationDecisions>
): ApplicationDecisions {
  return {
    ...json,
    decisions: json.decisions.map((decision) => ({
      ...decision,
      sentDate: LocalDate.parseIso(decision.sentDate),
      resolved: LocalDate.parseNullableIso(decision.resolved)
    }))
  }
}

export function deserializeDecision(json: JsonOf<Decision>): Decision {
  return {
    ...json,
    startDate: LocalDate.parseIso(json.startDate).format(),
    endDate: LocalDate.parseIso(json.endDate).format(),
    sentDate: LocalDate.parseIso(json.sentDate)
  }
}

export interface Decision {
  id: UUID
  type: DecisionType
  startDate: string
  endDate: string
  unit: DecisionUnit
  applicationId: UUID
  childId: UUID
  childName: string
  documentKey: string | null
  decisionNumber: number
  sentDate: LocalDate
  status: DecisionStatus
}
