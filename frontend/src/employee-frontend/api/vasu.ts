// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from '../../lib-common/api'
import { JsonOf } from '../../lib-common/json'
import { UUID } from '../types'
import {
  deserializeVasuDocumentSummary,
  VasuDocumentSummary
} from '../types/vasu'
import { client } from './client'

export async function getVasuDocumentSummaries(
  childId: UUID
): Promise<Result<VasuDocumentSummary[]>> {
  return client
    .get<JsonOf<VasuDocumentSummary[]>>(`/children/${childId}/vasu-summaries`)
    .then((res) => Success.of(res.data.map(deserializeVasuDocumentSummary)))
    .catch((e) => Failure.fromError(e))
}
