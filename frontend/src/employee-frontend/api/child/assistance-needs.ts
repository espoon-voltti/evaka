// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import type {
  AssistanceBasisOption,
  AssistanceNeedRequest
} from 'lib-common/generated/api-types/assistanceneed'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import type { AssistanceNeed, AssistanceNeedResponse } from '../../types/child'
import { client } from '../client'

export async function createAssistanceNeed(
  childId: UUID,
  assistanceNeedData: AssistanceNeedRequest
): Promise<Result<AssistanceNeed>> {
  return client
    .post<JsonOf<AssistanceNeed>>(`/children/${childId}/assistance-needs`, {
      ...assistanceNeedData,
      startDate: assistanceNeedData.startDate.formatIso(),
      endDate: assistanceNeedData.endDate.formatIso()
    })
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      startDate: LocalDate.parseIso(data.startDate),
      endDate: LocalDate.parseIso(data.endDate),
      bases: new Set(data.bases)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getAssistanceNeeds(
  childId: UUID
): Promise<Result<AssistanceNeedResponse[]>> {
  return client
    .get<JsonOf<AssistanceNeedResponse[]>>(
      `/children/${childId}/assistance-needs`
    )
    .then((res) =>
      res.data.map((data) => ({
        ...data,
        need: {
          ...data.need,
          startDate: LocalDate.parseIso(data.need.startDate),
          endDate: LocalDate.parseIso(data.need.endDate),
          bases: new Set(data.need.bases)
        }
      }))
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function updateAssistanceNeed(
  assistanceNeedId: UUID,
  assistanceNeedData: AssistanceNeedRequest
): Promise<Result<AssistanceNeed>> {
  return client
    .put<JsonOf<AssistanceNeed>>(`/assistance-needs/${assistanceNeedId}`, {
      ...assistanceNeedData,
      startDate: assistanceNeedData.startDate.formatIso(),
      endDate: assistanceNeedData.endDate.formatIso()
    })
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      startDate: LocalDate.parseIso(data.startDate),
      endDate: LocalDate.parseIso(data.endDate),
      bases: new Set(data.bases)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function removeAssistanceNeed(
  assistanceNeedId: UUID
): Promise<Result<null>> {
  return client
    .delete(`/assistance-needs/${assistanceNeedId}`)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function getAssistanceBasisOptions(): Promise<
  Result<AssistanceBasisOption[]>
> {
  return client
    .get<JsonOf<AssistanceBasisOption[]>>('/assistance-basis-options')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
