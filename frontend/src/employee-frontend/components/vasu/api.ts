// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import { client } from '../../api/client'
import { mapVasuContent } from './vasu-content'
import LocalDate from 'lib-common/local-date'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  AuthorsContent,
  EvaluationDiscussionContent,
  VasuContent,
  VasuDiscussionContent,
  VasuDocument,
  VasuDocumentEvent,
  VasuDocumentEventType,
  VasuDocumentSummary
} from 'lib-common/generated/api-types/vasu'
import { UUID } from 'lib-common/types'

const mapVasuDocumentEvent = (
  e: JsonOf<VasuDocumentEvent>
): VasuDocumentEvent => ({ ...e, created: new Date(e.created) })

const mapVasuDocumentResponse = ({
  events,
  modifiedAt,
  templateRange,
  basics,
  content,
  vasuDiscussionContent,
  evaluationDiscussionContent,
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
    .get<JsonOf<VasuDocumentSummary[]>>(`/children/${childId}/vasu-summaries`)
    .then((res) =>
      Success.of(
        res.data.map(({ events, modifiedAt, ...rest }) => ({
          ...rest,
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

export interface EditFollowupEntryParams {
  documentId: UUID
  entryId?: UUID
  text: string
}
export async function editFollowupEntry({
  documentId,
  entryId,
  text
}: EditFollowupEntryParams): Promise<Result<null>> {
  return entryId
    ? client
        .post(`/vasu/${documentId}/edit-followup/${entryId}`, {
          text
        })
        .then(() => Success.of(null))
        .catch((e) => Failure.fromError(e))
    : Failure.of({ message: 'cannot edit without entry ID' })
}

export interface EditFollowupEntryAllowedParams {
  documentId: UUID
  entryId?: UUID
}
export async function getEditFollowupEntryAllowed({
  documentId,
  entryId
}: EditFollowupEntryAllowedParams): Promise<Result<boolean>> {
  return entryId
    ? client
        .get(`/vasu/${documentId}/edit-followup-allowed/${entryId}`)
        .then((res) => Success.of(res.data))
        .catch((e) => Failure.fromError(e))
    : Failure.of({ message: 'cannot edit without entry ID' })
}
