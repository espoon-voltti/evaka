// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'types'
import { Failure, Result, Success } from 'api/index'
import {
  AssistanceAction,
  AssistanceActionType,
  AssistanceMeasure
} from 'types/child'
import { client } from 'api/client'
import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'

export interface AssistanceActionRequest {
  startDate: LocalDate
  endDate: LocalDate
  actions: AssistanceActionType[]
  otherAction: string
  measures: AssistanceMeasure[]
}

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
): Promise<Result<AssistanceAction[]>> {
  return client
    .get<JsonOf<AssistanceAction[]>>(`/children/${childId}/assistance-actions`)
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        startDate: LocalDate.parseIso(data.startDate),
        endDate: LocalDate.parseIso(data.endDate),
        actions: new Set(data.actions),
        measures: new Set(data.measures)
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
