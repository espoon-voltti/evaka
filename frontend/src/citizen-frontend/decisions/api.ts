// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type {
  ApplicationDecisions,
  DecisionWithValidStartDatePeriod
} from 'lib-common/generated/api-types/application'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import { client } from '../api-client'

export async function getDecisions(): Promise<ApplicationDecisions[]> {
  return client
    .get<JsonOf<ApplicationDecisions[]>>('/citizen/decisions')
    .then((res) => res.data.map(deserializeApplicationDecisions))
}

function deserializeApplicationDecisions(
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

export function getDecisionsOfApplication(
  applicationId: UUID
): Promise<DecisionWithValidStartDatePeriod[]> {
  return client
    .get<JsonOf<DecisionWithValidStartDatePeriod[]>>(
      `/citizen/applications/${applicationId}/decisions`
    )
    .then((res) =>
      res.data.map(({ decision, validRequestedStartDatePeriod }) => ({
        decision: {
          ...decision,
          startDate: LocalDate.parseIso(decision.startDate),
          endDate: LocalDate.parseIso(decision.endDate),
          sentDate: LocalDate.parseNullableIso(decision.sentDate),
          requestedStartDate: LocalDate.parseNullableIso(
            decision.requestedStartDate
          ),
          resolved: LocalDate.parseNullableIso(decision.resolved)
        },
        validRequestedStartDatePeriod: FiniteDateRange.parseJson(
          validRequestedStartDatePeriod
        )
      }))
    )
}

export function acceptDecision({
  applicationId,
  ...body
}: {
  applicationId: UUID
  decisionId: UUID
  requestedStartDate: LocalDate
}): Promise<void> {
  return client
    .post(
      `/citizen/applications/${applicationId}/actions/accept-decision`,
      body
    )
    .then(() => undefined)
}

export function rejectDecision({
  applicationId,
  decisionId
}: {
  applicationId: UUID
  decisionId: UUID
}): Promise<void> {
  return client
    .post(`/citizen/applications/${applicationId}/actions/reject-decision`, {
      decisionId
    })
    .then(() => undefined)
}

export function getApplicationNotifications(): Promise<number> {
  return client
    .get<JsonOf<number>>('/citizen/applications/by-guardian/notifications')
    .then((res) => res.data)
}
