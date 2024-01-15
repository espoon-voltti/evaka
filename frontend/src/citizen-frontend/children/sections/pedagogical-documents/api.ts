// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'citizen-frontend/api-client'
import { PedagogicalDocumentCitizen } from 'lib-common/generated/api-types/pedagogicaldocument'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

const deserializePedagogicalDocument = (
  data: JsonOf<PedagogicalDocumentCitizen>
): PedagogicalDocumentCitizen => ({
  ...data,
  created: HelsinkiDateTime.parseIso(data.created)
})

export function getPedagogicalDocuments(
  childId: UUID
): Promise<PedagogicalDocumentCitizen[]> {
  return client
    .get<
      JsonOf<PedagogicalDocumentCitizen[]>
    >(`/citizen/children/${childId}/pedagogical-documents`)
    .then((res) => res.data.map(deserializePedagogicalDocument))
}

export function getUnreadPedagogicalDocumentsCount(): Promise<
  Record<UUID, number>
> {
  return client
    .get<
      JsonOf<Record<UUID, number>>
    >(`/citizen/pedagogical-documents/unread-count`)
    .then((res) => res.data)
}

export function markPedagogicalDocumentAsRead(documentId: UUID): Promise<void> {
  return client
    .post(`/citizen/pedagogical-documents/${documentId}/mark-read`)
    .then(() => undefined)
}
