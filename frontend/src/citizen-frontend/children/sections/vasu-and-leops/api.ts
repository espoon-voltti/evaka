// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'citizen-frontend/api-client'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  CitizenGetVasuDocumentResponse,
  CitizenGetVasuDocumentSummariesResponse,
  VasuDocument,
  VasuDocumentEvent
} from 'lib-common/generated/api-types/vasu'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { mapVasuContent } from './vasu/vasu-content'

const mapVasuDocumentEvent = (
  e: JsonOf<VasuDocumentEvent>
): VasuDocumentEvent => ({
  ...e,
  created: HelsinkiDateTime.parseIso(e.created)
})

export function getChildVasuSummaries(
  childId: UUID
): Promise<CitizenGetVasuDocumentSummariesResponse> {
  return client
    .get<
      JsonOf<CitizenGetVasuDocumentSummariesResponse>
    >(`/citizen/vasu/children/${childId}/vasu-summaries`)
    .then((res) => ({
      data: res.data.data.map(
        ({ events, modifiedAt, publishedAt, ...rest }) => ({
          ...rest,
          events: events.map(mapVasuDocumentEvent),
          modifiedAt: HelsinkiDateTime.parseIso(modifiedAt),
          publishedAt: publishedAt
            ? HelsinkiDateTime.parseIso(publishedAt)
            : null
        })
      ),
      permissionToShareRequired: res.data.permissionToShareRequired
    }))
}

const mapVasuDocumentResponse = ({
  events,
  modifiedAt,
  templateRange,
  basics,
  content,
  publishedAt,
  ...rest
}: JsonOf<VasuDocument>): VasuDocument => ({
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
})

export function getVasuDocument(
  id: UUID
): Promise<CitizenGetVasuDocumentResponse> {
  return client
    .get<JsonOf<CitizenGetVasuDocumentResponse>>(`/citizen/vasu/${id}`)
    .then((res) => ({
      vasu: mapVasuDocumentResponse(res.data.vasu),
      permissionToShareRequired: res.data.permissionToShareRequired,
      guardianHasGivenPermissionToShare:
        res.data.guardianHasGivenPermissionToShare
    }))
}

export function getUnreadVasuDocumentsCount(): Promise<Record<UUID, number>> {
  return client
    .get<JsonOf<Record<UUID, number>>>(`/citizen/vasu/children/unread-count`)
    .then((res) => res.data)
}

export function givePermissionToShareVasu(documentId: UUID): Promise<void> {
  return client
    .post(`/citizen/vasu/${documentId}/give-permission-to-share`)
    .then(() => undefined)
}
