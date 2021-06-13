// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from '../../lib-common/api'
import { JsonOf } from '../../lib-common/json'
import { UUID } from '../types'
import {
  deserializeVasuDocumentSummary,
  deserializeVasuTemplate,
  deserializeVasuTemplateSummary,
  VasuDocumentSummary,
  VasuTemplate,
  VasuTemplateSummary
} from '../types/vasu'
import { client } from './client'
import { VasuContent } from './child/vasu'
import FiniteDateRange from '../../lib-common/finite-date-range'

export async function getVasuDocumentSummaries(
  childId: UUID
): Promise<Result<VasuDocumentSummary[]>> {
  return client
    .get<JsonOf<VasuDocumentSummary[]>>(`/children/${childId}/vasu-summaries`)
    .then((res) => Success.of(res.data.map(deserializeVasuDocumentSummary)))
    .catch((e) => Failure.fromError(e))
}

export async function getVasuTemplateSummaries(): Promise<
  Result<VasuTemplateSummary[]>
> {
  return client
    .get<JsonOf<VasuTemplateSummary[]>>(`/vasu/templates`)
    .then((res) => Success.of(res.data.map(deserializeVasuTemplateSummary)))
    .catch((e) => Failure.fromError(e))
}

export async function getVasuTemplate(id: UUID): Promise<Result<VasuTemplate>> {
  return client
    .get<JsonOf<VasuTemplate>>(`/vasu/templates/${id}`)
    .then((res) => Success.of(deserializeVasuTemplate(res.data)))
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
