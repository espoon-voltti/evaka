// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'employee-frontend/types'
import { Result, Success, Failure } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import FiniteDateRange from '../../../../lib-common/finite-date-range'
import { client } from '../../../api/client'
import { VasuContent } from '../vasu-content'

export interface VasuTemplateSummary {
  id: UUID
  name: string
  valid: FiniteDateRange
  documentCount: number
}

export interface VasuTemplate {
  id: UUID
  name: string
  valid: FiniteDateRange
  content: VasuContent
}

export async function createVasuTemplate(
  name: string,
  valid: FiniteDateRange
): Promise<Result<null>> {
  return client
    .post(`/vasu/templates`, {
      name,
      valid
    })
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function getVasuTemplateSummaries(): Promise<
  Result<VasuTemplateSummary[]>
> {
  return client
    .get<JsonOf<VasuTemplateSummary[]>>(`/vasu/templates`)
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
        valid: FiniteDateRange.parseJson(res.data.valid)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function updateVasuTemplateContents(
  id: UUID,
  content: VasuContent
): Promise<Result<null>> {
  return client
    .put(`/vasu/templates/${id}/content`, content)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

export async function deleteVasuTemplate(id: UUID): Promise<Result<null>> {
  return client
    .delete(`/vasu/templates/${id}`)
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}
