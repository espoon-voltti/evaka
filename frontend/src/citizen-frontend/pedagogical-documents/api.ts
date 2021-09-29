// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { client } from '../api-client'
import { JsonOf } from 'lib-common/json'
import { PedagogicalDocument } from 'lib-common/generated/api-types/pedagogicaldocument'

export async function getPedagogicalDocuments(): Promise<
  Result<PedagogicalDocument[]>
> {
  return client
    .get<JsonOf<PedagogicalDocument[]>>('/citizen/pedagogical-documents/')
    .then((res) => res.data.map(deserializePedagogicalDocument))
    .then((data) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export function deserializePedagogicalDocument(
  data: JsonOf<PedagogicalDocument>
): PedagogicalDocument {
  const created = new Date(data.created)
  const updated = new Date(data.updated)
  return { ...data, created, updated }
}
