// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  AssistanceActionOption,
  AssistanceActionRequest
} from 'lib-common/generated/api-types/assistanceaction'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { AssistanceAction, AssistanceActionResponse } from '../../types/child'
import { client } from '../client'

export async function createAssistanceAction(
  childId: UUID,
  assistanceActionData: AssistanceActionRequest
): Promise<Result<AssistanceAction>> {
  return client
    .post<JsonOf<AssistanceAction>>(`/children/${childId}/assistance-actions`, {
      ...assistanceActionData,
      startDate: assistanceActionData.startDate.formatIso(),
      endDate: assistanceActionData.endDate.formatIso()
    })
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      startDate: LocalDate.parseIso(data.startDate),
      endDate: LocalDate.parseIso(data.endDate),
      actions: new Set(data.actions),
      measures: new Set(data.measures)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getAssistanceActions(
  childId: UUID
): Promise<Result<AssistanceActionResponse[]>> {
  return client
    .get<JsonOf<AssistanceActionResponse[]>>(
      `/children/${childId}/assistance-actions`
    )
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        action: {
          ...data.action,
          startDate: LocalDate.parseIso(data.action.startDate),
          endDate: LocalDate.parseIso(data.action.endDate),
          actions: new Set(data.action.actions),
          measures: new Set(data.action.measures)
        }
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function updateAssistanceAction(
  assistanceActionId: UUID,
  assistanceActionData: AssistanceActionRequest
): Promise<Result<AssistanceAction>> {
  return client
    .put<JsonOf<AssistanceAction>>(
      `/assistance-actions/${assistanceActionId}`,
      {
        ...assistanceActionData,
        startDate: assistanceActionData.startDate.formatIso(),
        endDate: assistanceActionData.endDate.formatIso()
      }
    )
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      startDate: LocalDate.parseIso(data.startDate),
      endDate: LocalDate.parseIso(data.endDate),
      actions: new Set(data.actions),
      measures: new Set(data.measures)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function removeAssistanceAction(
  assistanceActionId: UUID
): Promise<Result<null>> {
  return client
    .delete(`/assistance-actions/${assistanceActionId}`)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function getAssistanceActionOptions(): Promise<
  Result<AssistanceActionOption[]>
> {
  return client
    .get<JsonOf<AssistanceActionOption[]>>('/assistance-action-options')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
