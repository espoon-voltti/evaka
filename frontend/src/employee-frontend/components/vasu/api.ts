// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'employee-frontend/types'
import { Failure, Result, Success } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import { client } from '../../api/client'
import { getDocumentState } from './vasu-events'
import { VasuContent } from './vasu-content'

export type VasuDocumentState = 'DRAFT' | 'READY' | 'REVIEWED' | 'CLOSED'
export type VasuDocumentEventType =
  | 'PUBLISHED'
  | 'MOVED_TO_READY'
  | 'RETURNED_TO_READY'
  | 'MOVED_TO_REVIEWED'
  | 'RETURNED_TO_REVIEWED'
  | 'MOVED_TO_CLOSED'

export interface VasuDocumentEvent {
  id: UUID
  created: Date
  eventType: VasuDocumentEventType
}
const mapVasuDocumentEvent = (
  e: JsonOf<VasuDocumentEvent>
): VasuDocumentEvent => ({ ...e, created: new Date(e.created) })

interface VasuDocumentSummaryResponse {
  id: UUID
  name: string
  modifiedAt: Date
  events: VasuDocumentEvent[]
}
export interface VasuDocumentSummary extends VasuDocumentSummaryResponse {
  documentState: VasuDocumentState
}

interface VasuDocumentResponse {
  id: UUID
  events: VasuDocumentEvent[]
  modifiedAt: Date
  vasuDiscussionDate?: Date
  evaluationDiscussionDate?: Date
  child: {
    id: UUID
    firstName: string
    lastName: string
  }
  templateName: string
  content: VasuContent
}

export interface VasuDocument extends VasuDocumentResponse {
  documentState: VasuDocumentState
}

const mapVasuDocumentResponse = ({
  evaluationDiscussionDate,
  events,
  modifiedAt,
  vasuDiscussionDate,
  ...rest
}: JsonOf<VasuDocumentResponse>): VasuDocument => ({
  ...rest,
  events: events.map(mapVasuDocumentEvent),
  documentState: getDocumentState(events),
  modifiedAt: new Date(modifiedAt),
  evaluationDiscussionDate: evaluationDiscussionDate
    ? new Date(evaluationDiscussionDate)
    : undefined,
  vasuDiscussionDate: vasuDiscussionDate
    ? new Date(vasuDiscussionDate)
    : undefined
})

export async function createVasuDocument(
  childId: UUID,
  templateId: UUID
): Promise<Result<UUID>> {
  return client
    .post<UUID>(`/vasu`, { childId, templateId })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getVasuDocumentSummaries(
  childId: UUID
): Promise<Result<VasuDocumentSummary[]>> {
  return client
    .get<JsonOf<VasuDocumentSummaryResponse[]>>(
      `/children/${childId}/vasu-summaries`
    )
    .then((res) =>
      Success.of(
        res.data.map(({ events, modifiedAt, ...rest }) => ({
          ...rest,
          documentState: getDocumentState(events),
          events: events.map(mapVasuDocumentEvent),
          modifiedAt: new Date(modifiedAt)
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

export interface PutVasuDocumentParams {
  documentId: UUID
  content: VasuContent
}

export async function putVasuDocument({
  documentId,
  content
}: PutVasuDocumentParams): Promise<Result<null>> {
  return client
    .put(`/vasu/${documentId}`, { content })
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
}: UpdateDocumentStateParams): Promise<Result<VasuDocumentEvent>> {
  return client
    .post<VasuDocumentEvent>(`/vasu/${documentId}/update-state`, { eventType })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
