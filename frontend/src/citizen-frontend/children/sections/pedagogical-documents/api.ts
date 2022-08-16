// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'citizen-frontend/api-client'
import { Failure, Result, Success } from 'lib-common/api'
import { PedagogicalDocumentCitizen } from 'lib-common/generated/api-types/pedagogicaldocument'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

export async function getPedagogicalDocuments(
  childId: UUID
): Promise<Result<PedagogicalDocumentCitizen[]>> {
  try {
    const data = await client
      .get<JsonOf<PedagogicalDocumentCitizen[]>>(
        `/citizen/children/${childId}/pedagogical-documents`
      )
      .then((res) => res.data.map(deserializePedagogicalDocument))
    return Success.of(data)
  } catch (e) {
    return Failure.fromError(e)
  }
}

const deserializePedagogicalDocument = (
  data: JsonOf<PedagogicalDocumentCitizen>
): PedagogicalDocumentCitizen => ({
  ...data,
  created: HelsinkiDateTime.parseIso(data.created)
})

export async function markPedagogicalDocumentRead(
  documentId: UUID
): Promise<Result<void>> {
  try {
    await client.post(`/citizen/pedagogical-documents/${documentId}/mark-read`)
    return Success.of()
  } catch (e) {
    return Failure.fromError(e)
  }
}

export async function getUnreadPedagogicalDocumentsCount(): Promise<
  Result<Record<UUID, number>>
> {
  try {
    const count = await client
      .get<JsonOf<Record<UUID, number>>>(
        `/citizen/pedagogical-documents/unread-count`
      )
      .then((res) => res.data)
    return Success.of(count)
  } catch (e) {
    return Failure.fromError(e)
  }
}
