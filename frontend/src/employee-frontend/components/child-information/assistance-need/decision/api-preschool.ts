// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AnnulAssistanceNeedDecisionRequest,
  AssistanceNeedDecisionStatus,
  AssistanceNeedPreschoolDecision,
  AssistanceNeedPreschoolDecisionBasicsResponse,
  AssistanceNeedPreschoolDecisionForm,
  AssistanceNeedPreschoolDecisionResponse,
  DecideAssistanceNeedPreschoolDecisionRequest
} from 'lib-common/generated/api-types/assistanceneed'
import { Employee } from 'lib-common/generated/api-types/pis'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../../../../api/client'

const deserializeAssistanceNeedPreschoolDecision = (
  json: JsonOf<AssistanceNeedPreschoolDecision>
): AssistanceNeedPreschoolDecision => ({
  ...json,
  child: {
    ...json.child,
    dateOfBirth: LocalDate.parseIso(json.child.dateOfBirth)
  },
  form: {
    ...json.form,
    validFrom: LocalDate.parseNullableIso(json.form.validFrom),
    guardiansHeardOn: LocalDate.parseNullableIso(json.form.guardiansHeardOn),
    basisDocumentPedagogicalReportDate: LocalDate.parseNullableIso(
      json.form.basisDocumentPedagogicalReportDate
    ),
    basisDocumentPsychologistStatementDate: LocalDate.parseNullableIso(
      json.form.basisDocumentPsychologistStatementDate
    ),
    basisDocumentSocialReportDate: LocalDate.parseNullableIso(
      json.form.basisDocumentSocialReportDate
    ),
    basisDocumentDoctorStatementDate: LocalDate.parseNullableIso(
      json.form.basisDocumentDoctorStatementDate
    )
  },
  sentForDecision: LocalDate.parseNullableIso(json.sentForDecision),
  decisionMade: LocalDate.parseNullableIso(json.decisionMade)
})

const deserializeAssistanceNeedPreschoolDecisionBasicsResponse = (
  json: JsonOf<AssistanceNeedPreschoolDecisionBasicsResponse>
): AssistanceNeedPreschoolDecisionBasicsResponse => ({
  ...json,
  decision: {
    ...json.decision,
    created: HelsinkiDateTime.parseIso(json.decision.created),
    validFrom: LocalDate.parseNullableIso(json.decision.validFrom),
    validTo: LocalDate.parseNullableIso(json.decision.validTo),
    sentForDecision: LocalDate.parseNullableIso(json.decision.sentForDecision),
    decisionMade: LocalDate.parseNullableIso(json.decision.decisionMade)
  }
})

export async function postAssistanceNeedPreschoolDecision(
  childId: UUID
): Promise<AssistanceNeedPreschoolDecision> {
  return client
    .post<
      JsonOf<AssistanceNeedPreschoolDecision>
    >(`/children/${childId}/assistance-need-preschool-decisions`)
    .then((res) => deserializeAssistanceNeedPreschoolDecision(res.data))
}

export async function getAssistanceNeedPreschoolDecisionBasics(
  childId: UUID
): Promise<AssistanceNeedPreschoolDecisionBasicsResponse[]> {
  return client
    .get<
      JsonOf<AssistanceNeedPreschoolDecisionBasicsResponse[]>
    >(`/children/${childId}/assistance-need-preschool-decisions`)
    .then((res) =>
      res.data.map(deserializeAssistanceNeedPreschoolDecisionBasicsResponse)
    )
}

export async function getAssistanceNeedPreschoolDecision(
  decisionId: UUID
): Promise<AssistanceNeedPreschoolDecisionResponse> {
  return client
    .get<
      JsonOf<AssistanceNeedPreschoolDecisionResponse>
    >(`/assistance-need-preschool-decisions/${decisionId}`)
    .then((res) => ({
      ...res.data,
      decision: deserializeAssistanceNeedPreschoolDecision(res.data.decision)
    }))
}

export async function putAssistanceNeedPreschoolDecision(
  decisionId: UUID,
  body: AssistanceNeedPreschoolDecisionForm
): Promise<void> {
  await client.put(`/assistance-need-preschool-decisions/${decisionId}`, body)
}

export async function putAssistanceNeedPreschoolDecisionSend(
  decisionId: UUID
): Promise<void> {
  await client.put(`/assistance-need-preschool-decisions/${decisionId}/send`)
}

export async function putAssistanceNeedPreschoolDecisionUnsend(
  decisionId: UUID
): Promise<void> {
  await client.put(`/assistance-need-preschool-decisions/${decisionId}/unsend`)
}

export async function putAssistanceNeedPreschoolDecisionMarkAsOpened(
  decisionId: UUID
): Promise<void> {
  await client.put(
    `/assistance-need-preschool-decisions/${decisionId}/mark-as-opened`
  )
}

export async function putAssistanceNeedPreschoolDecisionDecide(
  decisionId: UUID,
  status: AssistanceNeedDecisionStatus
): Promise<void> {
  const body: DecideAssistanceNeedPreschoolDecisionRequest = {
    status
  }
  await client.put(
    `/assistance-need-preschool-decisions/${decisionId}/decide`,
    body
  )
}

export async function putAssistanceNeedPreschoolDecisionAnnul(
  decisionId: UUID,
  reason: string
): Promise<void> {
  const body: AnnulAssistanceNeedDecisionRequest = {
    reason
  }
  await client.put(
    `/assistance-need-preschool-decisions/${decisionId}/annul`,
    body
  )
}

export async function deleteAssistanceNeedPreschoolDecision(
  id: UUID
): Promise<void> {
  await client.delete(`assistance-need-preschool-decisions/${id}`)
}

export async function getAssistanceNeedPreschoolDecisionMakerOptions(
  decisionId: UUID
): Promise<Employee[]> {
  return client
    .get<
      JsonOf<Employee[]>
    >(`/assistance-need-preschool-decisions/${decisionId}/decision-maker-options`)
    .then((res) =>
      res.data.map((employee) => ({
        ...employee,
        created: HelsinkiDateTime.parseIso(employee.created),
        updated: employee.updated
          ? HelsinkiDateTime.parseIso(employee.updated)
          : null
      }))
    )
}
