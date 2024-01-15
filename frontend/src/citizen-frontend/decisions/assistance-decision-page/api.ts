// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'citizen-frontend/api-client'
import DateRange from 'lib-common/date-range'
import {
  AssistanceNeedDecision,
  AssistanceNeedDecisionCitizenListItem,
  UnreadAssistanceNeedDecisionItem
} from 'lib-common/generated/api-types/assistanceneed'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

const mapToAssistanceNeedDecision = (
  data: JsonOf<AssistanceNeedDecision>
): AssistanceNeedDecision => ({
  ...data,
  validityPeriod: DateRange.parseJson(data.validityPeriod),
  decisionMade: LocalDate.parseNullableIso(data.decisionMade),
  guardiansHeardOn: LocalDate.parseNullableIso(data.guardiansHeardOn),
  sentForDecision: LocalDate.parseNullableIso(data.sentForDecision),
  child: data.child && {
    ...data.child,
    dateOfBirth: LocalDate.parseNullableIso(data.child.dateOfBirth)
  }
})

export function getAssistanceDecisions(): Promise<
  AssistanceNeedDecisionCitizenListItem[]
> {
  return client
    .get<
      JsonOf<AssistanceNeedDecisionCitizenListItem[]>
    >(`/citizen/assistance-need-decisions`)
    .then(({ data }) =>
      data.map((decision) => ({
        ...decision,
        decisionMade: LocalDate.parseIso(decision.decisionMade),
        validityPeriod: DateRange.parseJson(decision.validityPeriod)
      }))
    )
}

export function getAssitanceDecision(
  id: UUID
): Promise<AssistanceNeedDecision> {
  return client
    .get<
      JsonOf<AssistanceNeedDecision>
    >(`/citizen/children/assistance-need-decision/${id}`)
    .then((res) => mapToAssistanceNeedDecision(res.data))
}

export function getAssistanceDecisionUnreadCounts(): Promise<
  UnreadAssistanceNeedDecisionItem[]
> {
  return client
    .get<
      JsonOf<UnreadAssistanceNeedDecisionItem[]>
    >(`/citizen/children/assistance-need-decisions/unread-counts`)
    .then((res) => res.data)
}

export function markAssistanceNeedDecisionAsRead(id: UUID): Promise<void> {
  return client
    .post(`/citizen/children/assistance-need-decision/${id}/read`)
    .then(() => undefined)
}
