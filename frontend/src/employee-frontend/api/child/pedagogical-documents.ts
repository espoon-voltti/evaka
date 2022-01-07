// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  PedagogicalDocument,
  PedagogicalDocumentPostBody
} from 'lib-common/generated/api-types/pedagogicaldocument'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../client'

export async function getChildPedagogicalDocuments(
  childId: UUID
): Promise<Result<PedagogicalDocument[]>> {
  return client
    .get<JsonOf<PedagogicalDocument[]>>(
      `/pedagogical-document/child/${childId}`
    )
    .then((res) =>
      Success.of(
        res.data.map(({ created, updated, ...rest }) => ({
          ...rest,
          created: new Date(created),
          updated: new Date(updated)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function createPedagogicalDocument(
  data: PedagogicalDocumentPostBody
): Promise<Result<PedagogicalDocument>> {
  return client
    .post(`/pedagogical-document`, data)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function updatePedagogicalDocument(
  documentId: UUID,
  data: PedagogicalDocumentPostBody
): Promise<Result<UUID>> {
  return client
    .put(`/pedagogical-document/${documentId}`, data)
    .then(() => Success.of(documentId))
    .catch((e) => Failure.fromError(e))
}

export async function deletePedagogicalDocument(
  documentId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/pedagogical-document/${documentId}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
