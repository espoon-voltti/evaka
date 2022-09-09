// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  ApplicationDecisions,
  DecisionWithValidStartDatePeriod
} from 'lib-common/generated/api-types/application'
import { AssistanceNeedDecisionCitizenListItem } from 'lib-common/generated/api-types/assistanceneed'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../api-client'

export async function getDecisions(): Promise<Result<ApplicationDecisions[]>> {
  return client
    .get<JsonOf<ApplicationDecisions[]>>('/citizen/decisions')
    .then((res) => res.data.map(deserializeApplicationDecisions))
    .then((data) => Success.of(data))
    .catch((e) => Failure.fromError(e))
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

export async function getApplicationDecisions(
  applicationId: UUID
): Promise<Result<DecisionWithValidStartDatePeriod[]>> {
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
          sentDate: LocalDate.parseIso(decision.sentDate),
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
    .then((data) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export async function acceptDecision(
  applicationId: UUID,
  decisionId: UUID,
  requestedStartDate: LocalDate
): Promise<Result<void>> {
  return client
    .post<void>(
      `/citizen/applications/${applicationId}/actions/accept-decision`,
      {
        decisionId,
        requestedStartDate
      }
    )
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function rejectDecision(
  applicationId: UUID,
  decisionId: UUID
): Promise<Result<void>> {
  const body = { decisionId }
  return client
    .post<void>(
      `/citizen/applications/${applicationId}/actions/reject-decision`,
      body
    )
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export function getAssistanceNeedDecisions(): Promise<
  Result<AssistanceNeedDecisionCitizenListItem[]>
> {
  return client
    .get<JsonOf<AssistanceNeedDecisionCitizenListItem[]>>(
      `/citizen/assistance-need-decisions`
    )
    .then(({ data }) =>
      Success.of(
        data.map((decision) => ({
          ...decision,
          decisionMade: LocalDate.parseIso(decision.decisionMade),
          validityPeriod: DateRange.parseJson(decision.validityPeriod)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}
