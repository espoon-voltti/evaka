// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'citizen-frontend/api-client'
import DateRange from 'lib-common/date-range'
import {
  AssistanceNeedPreschoolDecision,
  AssistanceNeedPreschoolDecisionCitizenListItem,
  UnreadAssistanceNeedDecisionItem
} from 'lib-common/generated/api-types/assistanceneed'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

const mapToAssistanceNeedPreschoolDecision = (
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

export async function getAssistanceNeedPreschoolDecisions(): Promise<
  AssistanceNeedPreschoolDecisionCitizenListItem[]
> {
  return client
    .get<
      JsonOf<AssistanceNeedPreschoolDecisionCitizenListItem[]>
    >(`/citizen/assistance-need-preschool-decisions`)
    .then(({ data }) =>
      data.map((decision) => ({
        ...decision,
        decisionMade: LocalDate.parseIso(decision.decisionMade),
        validityPeriod: DateRange.parseJson(decision.validityPeriod)
      }))
    )
}

export async function getAssistanceNeedPreschoolDecision(
  id: UUID
): Promise<AssistanceNeedPreschoolDecision> {
  return client
    .get<
      JsonOf<AssistanceNeedPreschoolDecision>
    >(`/citizen/children/assistance-need-preschool-decisions/${id}`)
    .then((res) => mapToAssistanceNeedPreschoolDecision(res.data))
}

export async function getAssistanceNeedPreschoolDecisionUnreadCounts(): Promise<
  UnreadAssistanceNeedDecisionItem[]
> {
  return client
    .get<
      JsonOf<UnreadAssistanceNeedDecisionItem[]>
    >(`/citizen/children/assistance-need-preschool-decisions/unread-counts`)
    .then((res) => res.data)
}

export async function markAssistanceNeedPreschoolDecisionAsRead(
  id: UUID
): Promise<void> {
  await client.put(
    `/citizen/children/assistance-need-preschool-decisions/${id}/read`
  )
}
