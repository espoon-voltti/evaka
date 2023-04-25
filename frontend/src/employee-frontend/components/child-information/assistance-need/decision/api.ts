// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Employee } from 'employee-frontend/types/employee'
import { Failure, Result, Success } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import {
  AssistanceNeedDecision,
  AssistanceNeedDecisionBasicsResponse,
  AssistanceNeedDecisionForm,
  AssistanceNeedDecisionResponse,
  AssistanceNeedDecisionStatus
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
  validityPeriod: DateRange.parseJson(data.validityPeriod),
  decisionMade: parseDate(data.decisionMade),
  guardiansHeardOn: parseDate(data.guardiansHeardOn),
  sentForDecision: parseDate(data.sentForDecision),
  child: data.child && {
    ...data.child,
    dateOfBirth: parseDate(data.child.dateOfBirth)
  }
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
    validityPeriod: DateRange.parseJson(data.decision.validityPeriod),
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

export function sendAssistanceNeedDecision(id: UUID): Promise<Result<void>> {
  return client
    .post(`/assistance-need-decision/${id}/send`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function revertToUnsentAssistanceNeedDecision(
  id: UUID
): Promise<Result<void>> {
  return client
    .post(`/assistance-need-decision/${id}/revert-to-unsent`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function decideAssistanceNeedDecision(
  id: UUID,
  status: Exclude<AssistanceNeedDecisionStatus, 'DRAFT'>
): Promise<Result<void>> {
  return client
    .post(`/assistance-need-decision/${id}/decide`, { status })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function annulAssistanceNeedDecision(
  id: UUID,
  reason: string
): Promise<Result<void>> {
  return client
    .post(`/assistance-need-decision/${id}/annul`, { reason })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function markAssistanceNeedDecisionAsOpened(
  id: UUID
): Promise<Result<void>> {
  return client
    .post(`/assistance-need-decision/${id}/mark-as-opened`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function updateAssistanceNeedDecisionDecisionMaker(
  id: UUID,
  title: string
): Promise<Result<void>> {
  return client
    .post(`/assistance-need-decision/${id}/update-decision-maker`, { title })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function getAssistanceDecisionMakerOptions(
  id: UUID
): Promise<Result<Employee[]>> {
  return client
    .get<JsonOf<Employee[]>>(
      `/assistance-need-decision/${id}/decision-maker-option`
    )
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        created: HelsinkiDateTime.parseIso(data.created),
        updated: HelsinkiDateTime.parseIso(data.updated)
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}
