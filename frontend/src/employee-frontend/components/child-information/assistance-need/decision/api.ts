// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  AssistanceNeedDecision,
  AssistanceNeedDecisionBasicsResponse,
  AssistanceNeedDecisionForm,
  AssistanceNeedDecisionResponse
} from 'lib-common/generated/api-types/assistanceneed'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../../../../api/client'

const parseDate = (str: string | null): LocalDate | null =>
  str ? LocalDate.parseIso(str) : null

const mapToAssistanceNeedDecision = (
  data: JsonOf<AssistanceNeedDecision>
): AssistanceNeedDecision => ({
  ...data,
  startDate: parseDate(data.startDate),
  endDate: parseDate(data.endDate),
  assistanceServicesTime:
    data.assistanceServicesTime !== null
      ? FiniteDateRange.parseJson(data.assistanceServicesTime)
      : null,
  decisionMade: parseDate(data.decisionMade),
  guardiansHeardOn: parseDate(data.guardiansHeardOn),
  sentForDecision: parseDate(data.sentForDecision)
})

const mapToAssistanceNeedDecisionResponse = (
  data: JsonOf<AssistanceNeedDecisionResponse>
): AssistanceNeedDecisionResponse => ({
  ...data,
  decision: mapToAssistanceNeedDecision(data.decision)
})

const mapToAssistanceNeedDecisionBasicsReponse = (
  data: JsonOf<AssistanceNeedDecisionBasicsResponse>
): AssistanceNeedDecisionBasicsResponse => ({
  ...data,
  decision: {
    ...data.decision,
    startDate: parseDate(data.decision.startDate),
    endDate: parseDate(data.decision.endDate),
    decisionMade: parseDate(data.decision.decisionMade),
    sentForDecision: parseDate(data.decision.sentForDecision),
    created: HelsinkiDateTime.parseIso(data.decision.created)
  }
})

export function getAssistanceNeedDecision(
  id: UUID
): Promise<Result<AssistanceNeedDecisionResponse>> {
  return client
    .get<JsonOf<AssistanceNeedDecisionResponse>>(
      `/assistance-need-decision/${id}`
    )
    .then((res) => Success.of(mapToAssistanceNeedDecisionResponse(res.data)))
    .catch((e) => Failure.fromError(e))
}

export function putAssistanceNeedDecision(
  id: UUID,
  data: AssistanceNeedDecisionForm
): Promise<Result<void>> {
  return client
    .put(`/assistance-need-decision/${id}`, {
      decision: data
    })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function getAssistanceNeedDecisions(
  childId: UUID
): Promise<Result<AssistanceNeedDecisionBasicsResponse[]>> {
  return client
    .get<JsonOf<AssistanceNeedDecisionBasicsResponse[]>>(
      `/children/${childId}/assistance-needs/decisions`
    )
    .then((res) =>
      Success.of(res.data.map(mapToAssistanceNeedDecisionBasicsReponse))
    )
    .catch((e) => Failure.fromError(e))
}

export function createAssistanceNeedDecision(
  childId: UUID,
  decision: AssistanceNeedDecisionForm
): Promise<Result<AssistanceNeedDecision>> {
  return client
    .post<JsonOf<AssistanceNeedDecision>>(
      `/children/${childId}/assistance-needs/decision`,
      { decision }
    )
    .then((res) => Success.of(mapToAssistanceNeedDecision(res.data)))
    .catch((e) => Failure.fromError(e))
}

export function deleteAssistanceNeedDecision(
  decisionId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/assistance-need-decision/${decisionId}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
