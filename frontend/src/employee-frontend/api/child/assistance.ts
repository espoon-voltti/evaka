// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import {
  AssistanceFactorUpdate,
  AssistanceResponse,
  DaycareAssistanceUpdate,
  OtherAssistanceMeasureUpdate,
  PreschoolAssistanceUpdate
} from 'lib-common/generated/api-types/assistance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../client'

export async function getAssistanceData(
  childId: UUID
): Promise<AssistanceResponse> {
  return client
    .get<
      JsonOf<AssistanceResponse>
    >(`/children/${encodeURIComponent(childId)}/assistance`)
    .then(({ data }) => ({
      assistanceFactors: data.assistanceFactors.map((row) => ({
        data: {
          ...row.data,
          validDuring: FiniteDateRange.parseJson(row.data.validDuring),
          modified: HelsinkiDateTime.parseIso(row.data.modified)
        },
        permittedActions: row.permittedActions
      })),
      daycareAssistances: data.daycareAssistances.map((row) => ({
        data: {
          ...row.data,
          validDuring: FiniteDateRange.parseJson(row.data.validDuring),
          modified: HelsinkiDateTime.parseIso(row.data.modified)
        },
        permittedActions: row.permittedActions
      })),
      preschoolAssistances: data.preschoolAssistances.map((row) => ({
        data: {
          ...row.data,
          validDuring: FiniteDateRange.parseJson(row.data.validDuring),
          modified: HelsinkiDateTime.parseIso(row.data.modified)
        },
        permittedActions: row.permittedActions
      })),
      assistanceActions: data.assistanceActions.map((row) => ({
        action: {
          ...row.action,
          startDate: LocalDate.parseIso(row.action.startDate),
          endDate: LocalDate.parseIso(row.action.endDate)
        },
        permittedActions: row.permittedActions
      })),
      otherAssistanceMeasures: data.otherAssistanceMeasures.map((row) => ({
        data: {
          ...row.data,
          validDuring: FiniteDateRange.parseJson(row.data.validDuring),
          modified: HelsinkiDateTime.parseIso(row.data.modified)
        },
        permittedActions: row.permittedActions
      }))
    }))
}

export async function createAssistanceFactor(
  childId: UUID,
  request: AssistanceFactorUpdate
): Promise<void> {
  await client.post<JsonOf<void>>(
    `/children/${encodeURIComponent(childId)}/assistance-factors`,
    request
  )
}

export async function updateAssistanceFactor(
  id: UUID,
  request: AssistanceFactorUpdate
): Promise<void> {
  await client.post<JsonOf<void>>(
    `/assistance-factors/${encodeURIComponent(id)}`,
    request
  )
}

export async function deleteAssistanceFactor(id: UUID): Promise<void> {
  await client.delete<JsonOf<void>>(
    `/assistance-factors/${encodeURIComponent(id)}`
  )
}

export async function createDaycareAssistance(
  childId: UUID,
  request: DaycareAssistanceUpdate
): Promise<void> {
  await client.post<JsonOf<void>>(
    `/children/${encodeURIComponent(childId)}/daycare-assistances`,
    request
  )
}

export async function updateDaycareAssistance(
  id: UUID,
  request: DaycareAssistanceUpdate
): Promise<void> {
  await client.post<JsonOf<void>>(
    `/daycare-assistances/${encodeURIComponent(id)}`,
    request
  )
}

export async function deleteDaycareAssistance(id: UUID): Promise<void> {
  await client.delete<JsonOf<void>>(
    `/daycare-assistances/${encodeURIComponent(id)}`
  )
}

export async function createPreschoolAssistance(
  childId: UUID,
  request: PreschoolAssistanceUpdate
): Promise<void> {
  await client.post<JsonOf<void>>(
    `/children/${encodeURIComponent(childId)}/preschool-assistances`,
    request
  )
}

export async function updatePreschoolAssistance(
  id: UUID,
  request: PreschoolAssistanceUpdate
): Promise<void> {
  await client.post<JsonOf<void>>(
    `/preschool-assistances/${encodeURIComponent(id)}`,
    request
  )
}

export async function deletePreschoolAssistance(id: UUID): Promise<void> {
  await client.delete<JsonOf<void>>(
    `/preschool-assistances/${encodeURIComponent(id)}`
  )
}

export async function createOtherAssistanceMeasure(
  childId: UUID,
  request: OtherAssistanceMeasureUpdate
): Promise<void> {
  await client.post<JsonOf<void>>(
    `/children/${encodeURIComponent(childId)}/other-assistance-measures`,
    request
  )
}

export async function updateOtherAssistanceMeasure(
  id: UUID,
  request: OtherAssistanceMeasureUpdate
): Promise<void> {
  await client.post<JsonOf<void>>(
    `/other-assistance-measures/${encodeURIComponent(id)}`,
    request
  )
}

export async function deleteOtherAssistanceMeasure(id: UUID): Promise<void> {
  await client.delete<JsonOf<void>>(
    `/other-assistance-measures/${encodeURIComponent(id)}`
  )
}
