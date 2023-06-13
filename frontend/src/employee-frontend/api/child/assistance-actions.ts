// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  AssistanceAction,
  AssistanceActionOption,
  AssistanceActionRequest
} from 'lib-common/generated/api-types/assistanceaction'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../client'

export async function createAssistanceAction(
  childId: UUID,
  assistanceActionData: AssistanceActionRequest
): Promise<AssistanceAction> {
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
      endDate: LocalDate.parseIso(data.endDate)
    }))
}

export async function updateAssistanceAction(
  assistanceActionId: UUID,
  assistanceActionData: AssistanceActionRequest
): Promise<AssistanceAction> {
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
      endDate: LocalDate.parseIso(data.endDate)
    }))
}

export async function removeAssistanceAction(
  assistanceActionId: UUID
): Promise<void> {
  await client.delete(`/assistance-actions/${assistanceActionId}`)
}

export async function getAssistanceActionOptions(): Promise<
  Result<AssistanceActionOption[]>
> {
  return client
    .get<JsonOf<AssistanceActionOption[]>>('/assistance-action-options')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
