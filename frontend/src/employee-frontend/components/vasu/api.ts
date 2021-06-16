// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'employee-frontend/types'
import { Result, Success, Failure } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import { client } from '../../api/client'
import { VasuContent } from './vasu-content'

export type VasuDocumentState = 'DRAFT' | 'CREATED' | 'REVIEWED' | 'CLOSED'

export interface VasuDocumentSummary {
  name: string
  state: VasuDocumentState
  modifiedAt: Date
  publishedAt: Date | null
  id: UUID
}

export interface VasuDocumentResponse {
  id: UUID
  child: {
    id: UUID
    firstName: string
    lastName: string
  }
  templateName: string
  content: VasuContent
}

export async function createVasuDocument(childId: UUID): Promise<Result<UUID>> {
  return client
    .post<VasuDocumentResponse>(`/vasu`, { childId })
    .then((res) => Success.of(res.data.id))
    .catch((e) => Failure.fromError(e))
}

export async function getVasuDocumentSummaries(
  childId: UUID
): Promise<Result<VasuDocumentSummary[]>> {
  return client
    .get<JsonOf<VasuDocumentSummary[]>>(`/children/${childId}/vasu-summaries`)
    .then((res) =>
      Success.of(
        res.data.map((doc) => ({
          ...doc,
          modifiedAt: new Date(doc.modifiedAt),
          publishedAt: doc.publishedAt ? new Date(doc.publishedAt) : null
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getVasuDocument(
  id: UUID
): Promise<Result<VasuDocumentResponse>> {
  return client
    .get<JsonOf<VasuDocumentResponse>>(`/vasu/${id}`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function putVasuDocument(
  documentId: UUID,
  content: VasuContent
): Promise<Result<null>> {
  return client
    .put(`/vasu/${documentId}`, { content })
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}
