// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result, Success, Failure } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  CreateTemplateRequest,
  VasuContent,
  VasuTemplate,
  VasuTemplateSummary
} from 'lib-common/generated/api-types/vasu'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

import { client } from '../../../api/client'
import { mapVasuContent } from '../vasu-content'

export const curriculumTypes = ['DAYCARE', 'PRESCHOOL'] as const
export const vasuLanguages = ['FI', 'SV'] as const

export async function createVasuTemplate(
  params: CreateTemplateRequest
): Promise<Result<UUID>> {
  return client
    .post<UUID>(`/vasu/templates`, params)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function editVasuTemplate(
  id: UUID,
  params: CreateTemplateRequest
): Promise<Result<UUID>> {
  return client
    .put<UUID>(`/vasu/templates/${id}`, params)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function copyVasuTemplate(
  id: UUID,
  name: string,
  valid: FiniteDateRange
): Promise<Result<UUID>> {
  return client
    .post<UUID>(`/vasu/templates/${id}/copy`, { name, valid })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getVasuTemplateSummaries(
  validOnly = false
): Promise<Result<VasuTemplateSummary[]>> {
  return client
    .get<JsonOf<VasuTemplateSummary[]>>(`/vasu/templates`, {
      params: { validOnly }
    })
    .then((res) =>
      Success.of(
        res.data.map((tmp) => ({
          ...tmp,
          valid: FiniteDateRange.parseJson(tmp.valid)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getVasuTemplate(id: UUID): Promise<Result<VasuTemplate>> {
  return client
    .get<JsonOf<VasuTemplate>>(`/vasu/templates/${id}`)
    .then((res) =>
      Success.of({
        ...res.data,
        content: mapVasuContent(res.data.content),
        valid: FiniteDateRange.parseJson(res.data.valid)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function updateVasuTemplateContents(
  id: UUID,
  content: VasuContent
): Promise<Result<void>> {
  return client
    .put(`/vasu/templates/${id}/content`, content)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function deleteVasuTemplate(id: UUID): Promise<Result<void>> {
  return client
    .delete(`/vasu/templates/${id}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
