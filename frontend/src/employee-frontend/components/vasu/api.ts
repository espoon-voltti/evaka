// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'employee-frontend/types'
import { Failure, Result, Success } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import { client } from '../../api/client'
import { getDocumentState } from './vasu-events'
import { VasuContent } from './vasu-content'
import LocalDate from 'lib-common/local-date'
import FiniteDateRange from 'lib-common/finite-date-range'

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

export interface AuthorsContent {
  primaryAuthor: AuthorInfo
  otherAuthors: AuthorInfo[]
}

export interface AuthorInfo {
  name: string
  title: string
  phone: string
}

export interface VasuDiscussionContent {
  discussionDate: LocalDate | null
  participants: string
  guardianViewsAndCollaboration: string
}

export interface EvaluationDiscussionContent {
  discussionDate: LocalDate | null
  participants: string
  guardianViewsAndCollaboration: string
  evaluation: string
}

export interface VasuBasics {
  child: {
    id: UUID
    firstName: string
    lastName: string
    dateOfBirth: LocalDate
  }
  guardians: {
    id: UUID
    firstName: string
    lastName: string
  }[]
  placements: {
    unitId: UUID
    unitName: string
    groupId: UUID
    groupName: string
    range: FiniteDateRange
  }[]
}

interface VasuDocumentResponse {
  id: UUID
  events: VasuDocumentEvent[]
  modifiedAt: Date
  basics: VasuBasics
  templateName: string
  templateRange: FiniteDateRange
  content: VasuContent
  authorsContent: AuthorsContent
  vasuDiscussionContent: VasuDiscussionContent
  evaluationDiscussionContent: EvaluationDiscussionContent
}

export interface VasuDocument extends VasuDocumentResponse {
  documentState: VasuDocumentState
}

const mapVasuDocumentResponse = ({
  events,
  modifiedAt,
  templateRange,
  basics,
  vasuDiscussionContent,
  evaluationDiscussionContent,
  ...rest
}: JsonOf<VasuDocumentResponse>): VasuDocument => ({
  ...rest,
  events: events.map(mapVasuDocumentEvent),
  documentState: getDocumentState(events),
  modifiedAt: new Date(modifiedAt),
  templateRange: FiniteDateRange.parseJson(templateRange),
  basics: {
    ...basics,
    child: {
      ...basics.child,
      dateOfBirth: LocalDate.parseIso(basics.child.dateOfBirth)
    },
    placements: basics.placements.map((pl) => ({
      ...pl,
      range: FiniteDateRange.parseJson(pl.range)
    }))
  },
  vasuDiscussionContent: {
    ...vasuDiscussionContent,
    discussionDate: LocalDate.parseNullableIso(
      vasuDiscussionContent.discussionDate
    )
  },
  evaluationDiscussionContent: {
    ...evaluationDiscussionContent,
    discussionDate: LocalDate.parseNullableIso(
      evaluationDiscussionContent.discussionDate
    )
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
  authorsContent: AuthorsContent
  vasuDiscussionContent: VasuDiscussionContent
  evaluationDiscussionContent: EvaluationDiscussionContent
}

export async function putVasuDocument({
  documentId,
  content,
  authorsContent,
  vasuDiscussionContent,
  evaluationDiscussionContent
}: PutVasuDocumentParams): Promise<Result<null>> {
  return client
    .put(`/vasu/${documentId}`, {
      content,
      authorsContent,
      vasuDiscussionContent,
      evaluationDiscussionContent
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
