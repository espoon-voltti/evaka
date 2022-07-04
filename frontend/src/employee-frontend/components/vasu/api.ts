// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  UpdateDocumentRequest,
  VasuDocument,
  VasuDocumentEvent,
  VasuDocumentEventType,
  VasuDocumentSummary
} from 'lib-common/generated/api-types/vasu'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../../api/client'

import { mapVasuContent } from './vasu-content'

const mapVasuDocumentEvent = (
  e: JsonOf<VasuDocumentEvent>
): VasuDocumentEvent => ({ ...e, created: new Date(e.created) })

const mapVasuDocumentResponse = ({
  events,
  modifiedAt,
  templateRange,
  basics,
  content,
  ...rest
}: JsonOf<VasuDocument>): VasuDocument => ({
  ...rest,
  content: mapVasuContent(content),
  events: events.map(mapVasuDocumentEvent),
  modifiedAt: new Date(modifiedAt),
  templateRange: FiniteDateRange.parseJson(templateRange),
  basics: {
    ...basics,
    child: {
      ...basics.child,
      dateOfBirth: LocalDate.parseIso(basics.child.dateOfBirth)
    },
    placements:
      basics.placements?.map((pl) => ({
        ...pl,
        range: FiniteDateRange.parseJson(pl.range)
      })) ?? null
  }
})

export async function createVasuDocument(
  childId: UUID,
  templateId: UUID
): Promise<Result<UUID>> {
  return client
    .post<UUID>(`/children/${childId}/vasu`, { templateId })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getVasuDocumentSummaries(
  childId: UUID
): Promise<Result<VasuDocumentSummary[]>> {
  return client
    .get<JsonOf<VasuDocumentSummary[]>>(`/children/${childId}/vasu-summaries`)
    .then((res) =>
      Success.of(
        res.data.map(({ events, modifiedAt, publishedAt, ...rest }) => ({
          ...rest,
          events: events.map(mapVasuDocumentEvent),
          modifiedAt: new Date(modifiedAt),
          publishedAt: publishedAt ? new Date(publishedAt) : null
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getVasuDocument(id: UUID): Promise<Result<VasuDocument>> {
  return client
    .get<JsonOf<VasuDocument>>(`/vasu/${id}`)
    .then((res) => Success.of(mapVasuDocumentResponse(res.data)))
    .catch((e) => Failure.fromError(e))
}

export interface PutVasuDocumentParams extends UpdateDocumentRequest {
  documentId: UUID
}

export async function putVasuDocument({
  documentId,
  content,
  childLanguage
}: PutVasuDocumentParams): Promise<Result<null>> {
  return client
    .put<UpdateDocumentRequest>(`/vasu/${documentId}`, {
      content,
      childLanguage
    })
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}

interface UpdateDocumentStateParams {
  documentId: UUID
  eventType: VasuDocumentEventType
}
export async function updateDocumentState({
  documentId,
  eventType
}: UpdateDocumentStateParams): Promise<Result<null>> {
  return client
    .post(`/vasu/${documentId}/update-state`, { eventType })
    .then(() => Success.of(null))
    .catch((e) => Failure.fromError(e))
}
