// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'citizen-frontend/api-client'
import { Failure, Result, Success } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import {
  AssistanceNeedDecision,
  AssistanceNeedDecisionCitizenListItem,
  UnreadAssistanceNeedDecisionItem
} from 'lib-common/generated/api-types/assistanceneed'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

export function getAssistanceNeedDecisions(
  childId: UUID
): Promise<Result<AssistanceNeedDecisionCitizenListItem[]>> {
  return client
    .get<JsonOf<AssistanceNeedDecisionCitizenListItem[]>>(
      `/citizen/children/${childId}/assistance-need-decisions`
    )
    .then(({ data }) =>
      Success.of(
        data.map((decision) => ({
          ...decision,
          decisionMade: parseDate(decision.decisionMade),
          validityPeriod: DateRange.parseJson(decision.validityPeriod)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

const parseDate = (str: string | null): LocalDate | null =>
  str ? LocalDate.parseIso(str) : null

const mapToAssistanceNeedDecision = (
  data: JsonOf<AssistanceNeedDecision>
): AssistanceNeedDecision => ({
  ...data,
  validityPeriod: DateRange.parseJson(data.validityPeriod),
  decisionMade: parseDate(data.decisionMade),
  guardiansHeardOn: parseDate(data.guardiansHeardOn),
  sentForDecision: parseDate(data.sentForDecision),
  child: data.child && {
    ...data.child,
    dateOfBirth: parseDate(data.child.dateOfBirth)
  }
})

export function getAssistanceNeedDecision(
  id: UUID
): Promise<Result<AssistanceNeedDecision>> {
  return client
    .get<JsonOf<AssistanceNeedDecision>>(
      `/citizen/children/assistance-need-decision/${id}`
    )
    .then((res) => Success.of(mapToAssistanceNeedDecision(res.data)))
    .catch((e) => Failure.fromError(e))
}

export function markAssistanceNeedDecisionAsRead(
  id: UUID
): Promise<Result<void>> {
  return client
    .post(`/citizen/children/assistance-need-decision/${id}/read`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function getAssistanceNeedDecisionUnreadCounts(): Promise<
  Result<UnreadAssistanceNeedDecisionItem[]>
> {
  return client
    .get<JsonOf<UnreadAssistanceNeedDecisionItem[]>>(
      `/citizen/children/assistance-need-decisions/unread-counts`
    )
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}
