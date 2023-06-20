// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AssistanceNeedPreschoolDecision,
  AssistanceNeedPreschoolDecisionBasicsResponse
} from 'lib-common/generated/api-types/assistanceneed'
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
    guardiansHeardOn: LocalDate.parseNullableIso(json.form.guardiansHeardOn)
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
    sentForDecision: LocalDate.parseNullableIso(json.decision.sentForDecision),
    decisionMade: LocalDate.parseNullableIso(json.decision.decisionMade)
  }
})

export async function postAssistanceNeedPreschoolDecision(
  childId: UUID
): Promise<AssistanceNeedPreschoolDecision> {
  return client
    .post<JsonOf<AssistanceNeedPreschoolDecision>>(
      `/children/${childId}/assistance-need-preschool-decisions`
    )
    .then((res) => deserializeAssistanceNeedPreschoolDecision(res.data))
}

export async function getAssistanceNeedPreschoolDecisionBasics(
  childId: UUID
): Promise<AssistanceNeedPreschoolDecisionBasicsResponse[]> {
  return client
    .get<JsonOf<AssistanceNeedPreschoolDecisionBasicsResponse[]>>(
      `/children/${childId}/assistance-need-preschool-decisions`
    )
    .then((res) =>
      res.data.map(deserializeAssistanceNeedPreschoolDecisionBasicsResponse)
    )
}

export async function deleteAssistanceNeedPreschoolDecision(
  id: UUID
): Promise<void> {
  await client.delete(`assistance-need-preschool-decisions/${id}`)
}
