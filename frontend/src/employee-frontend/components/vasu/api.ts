// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  UpdateDocumentRequest,
  VasuDocumentEvent,
  VasuDocumentEventType,
  VasuDocumentSummaryWithPermittedActions,
  VasuDocumentWithPermittedActions
} from 'lib-common/generated/api-types/vasu'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../../api/client'

import { mapVasuContent } from './vasu-content'

const mapVasuDocumentEvent = (
  e: JsonOf<VasuDocumentEvent>
): VasuDocumentEvent => ({
  ...e,
  created: HelsinkiDateTime.parseIso(e.created)
})

const mapVasuDocumentResponse = ({
  data: {
    events,
    modifiedAt,
    templateRange,
    basics,
    content,
    publishedAt,
    ...rest
  },
  permittedActions
}: JsonOf<VasuDocumentWithPermittedActions>): VasuDocumentWithPermittedActions => ({
  data: {
    ...rest,
    content: mapVasuContent(content),
    events: events.map(mapVasuDocumentEvent),
    modifiedAt: HelsinkiDateTime.parseIso(modifiedAt),
    templateRange: FiniteDateRange.parseJson(templateRange),
    publishedAt:
      publishedAt !== null ? HelsinkiDateTime.parseIso(publishedAt) : null,
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
  },
  permittedActions
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
): Promise<VasuDocumentSummaryWithPermittedActions[]> {
  return client
    .get<
      JsonOf<VasuDocumentSummaryWithPermittedActions[]>
    >(`/children/${childId}/vasu-summaries`)
    .then((res) =>
      res.data.map(
        ({
          data: { events, modifiedAt, publishedAt, ...rest },
          permittedActions
        }) => ({
          data: {
            ...rest,
            events: events.map(mapVasuDocumentEvent),
            modifiedAt: HelsinkiDateTime.parseIso(modifiedAt),
            publishedAt: publishedAt
              ? HelsinkiDateTime.parseIso(publishedAt)
              : null
          },
          permittedActions
        })
      )
    )
}

export async function getVasuDocument(
  id: UUID
): Promise<Result<VasuDocumentWithPermittedActions>> {
  return client
    .get<JsonOf<VasuDocumentWithPermittedActions>>(`/vasu/${id}`)
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

export interface UpdateDocumentStateParams {
  documentId: UUID
  eventType: VasuDocumentEventType
}

export async function updateDocumentState({
  documentId,
  eventType
}: UpdateDocumentStateParams): Promise<void> {
  return client
    .post(`/vasu/${documentId}/update-state`, { eventType })
    .then(() => undefined)
}

export async function deleteVasuDocument(documentId: UUID): Promise<void> {
  return client.delete(`/vasu/${documentId}`).then(() => undefined)
}
