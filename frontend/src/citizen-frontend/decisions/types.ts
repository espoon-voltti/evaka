// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { JsonOf } from 'lib-common/json'

export type DecisionType =
  | 'CLUB'
  | 'DAYCARE'
  | 'DAYCARE_PART_TIME'
  | 'PRESCHOOL'
  | 'PRESCHOOL_DAYCARE'
  | 'PREPARATORY_EDUCATION'

export type DecisionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

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

export interface ApplicationDecisions {
  applicationId: UUID
  childName: string
  decisions: DecisionSummary[]
}

export interface DecisionSummary {
  decisionId: UUID
  type: DecisionType
  status: DecisionStatus
  sentDate: LocalDate
  resolved: LocalDate | null
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

export interface DecisionUnit {
  id: UUID
  name: string
  daycareDecisionName: string
  preschoolDecisionName: string
  manager: string | null
  streetAddress: string
  postalCode: string
  postOffice: string
  approverName: string
  decisionHandler: string
  decisionHandlerAddress: string
}
