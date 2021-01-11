// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '@evaka/lib-common/src/types'
import { Failure, Result, Success } from '@evaka/lib-common/src/api'
import { JsonOf } from '@evaka/lib-common/src/json'
import {
  ApplicationDecisions,
  Decision,
  deserializeApplicationDecisions,
  deserializeDecision
} from '~decisions/types'
import { client } from '~api-client'
import LocalDate from '@evaka/lib-common/src/local-date'

export async function getDecisions(): Promise<Result<ApplicationDecisions[]>> {
  return client
    .get<JsonOf<ApplicationDecisions[]>>('/citizen/decisions')
    .then((res) => res.data.map(deserializeApplicationDecisions))
    .then((data) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export async function getApplicationDecisions(
  applicationId: UUID
): Promise<Result<Decision[]>> {
  return client
    .get<JsonOf<Decision[]>>(`/citizen/applications/${applicationId}/decisions`)
    .then((res) => res.data.map(deserializeDecision))
    .then((data) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export async function acceptDecision(
  applicationId: UUID,
  decisionId: UUID,
  requestedStartDate: LocalDate
): Promise<Result<void>> {
  const body = { decisionId, requestedStartDate }
  return client
    .post<void>(
      `/citizen/applications/${applicationId}/actions/accept-decision`,
      body
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
