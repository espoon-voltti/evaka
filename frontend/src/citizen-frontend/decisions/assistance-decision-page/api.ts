// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'citizen-frontend/api-client'
import { Failure, Result, Success } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { AssistanceNeedDecision } from 'lib-common/generated/api-types/assistanceneed'
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
