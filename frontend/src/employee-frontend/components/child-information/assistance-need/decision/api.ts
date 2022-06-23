// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { AssistanceNeedDecision } from 'lib-common/generated/api-types/assistanceneed'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../../../../api/client'

import { AssistanceNeedDecisionForm } from './assistance-need-decision-form'

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

export function getAssistanceNeedDecision(
  childId: UUID,
  id: UUID
): Promise<Result<AssistanceNeedDecision>> {
  return client
    .get<JsonOf<AssistanceNeedDecision>>(
      `/children/${childId}/assistance-needs/decision/${id}`
    )
    .then((res) => Success.of(mapToAssistanceNeedDecision(res.data)))
    .catch((e) => Failure.fromError(e))
}

export function putAssistanceNeedDecision(
  childId: UUID,
  id: UUID,
  data: AssistanceNeedDecisionForm
): Promise<Result<void>> {
  return client
    .put(`/children/${childId}/assistance-needs/decision/${id}`, {
      decision: data
    })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
