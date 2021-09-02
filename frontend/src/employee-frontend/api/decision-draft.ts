// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../types'
import { Failure, Result, Success } from 'lib-common/api'
import { client } from './client'
import {
  DecisionDraftGroup,
  DecisionDraftUpdate,
  DecisionUnit
} from '../types/decision'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

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
): Promise<void> {
  return client.put(`/v2/applications/${id}/decision-drafts`, updatedDrafts)
}

export function getDecisionUnits(): Promise<Result<DecisionUnit[]>> {
  return client
    .get<JsonOf<DecisionUnit[]>>('/decisions2/units')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
