// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import type {
  DecisionDraftUpdate,
  DecisionUnit
} from 'lib-common/generated/api-types/decision'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import type { DecisionDraftGroup } from '../types/decision'

import { client } from './client'

export async function getDecisionDrafts(
  id: UUID
): Promise<Result<DecisionDraftGroup>> {
  return client
    .get<JsonOf<DecisionDraftGroup>>(`/v2/applications/${id}/decision-drafts`)
    .then((res) => {
      return res.data
    })
    .then((data) => {
      return {
        ...data,
        decisions: data.decisions.map((decisionDraft) => ({
          ...decisionDraft,
          startDate: LocalDate.parseIso(decisionDraft.startDate),
          endDate: LocalDate.parseIso(decisionDraft.endDate)
        }))
      }
    })
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function updateDecisionDrafts(
  id: UUID,
  updatedDrafts: DecisionDraftUpdate[]
): Promise<Result<void>> {
  return client
    .put(`/v2/applications/${id}/decision-drafts`, updatedDrafts)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function getDecisionUnits(): Promise<Result<DecisionUnit[]>> {
  return client
    .get<JsonOf<DecisionUnit[]>>('/decisions2/units')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
