// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'citizen-frontend/api-client'
import DateRange from 'lib-common/date-range'
import {
  ChildDocumentCitizenSummary,
  ChildDocumentDetails
} from 'lib-common/generated/api-types/document'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

const deserializeChildDocumentSummary = (
  data: JsonOf<ChildDocumentCitizenSummary>
): ChildDocumentCitizenSummary => ({
  ...data,
  publishedAt: HelsinkiDateTime.parseIso(data.publishedAt)
})

const deserializeChildDocumentDetails = (
  data: JsonOf<ChildDocumentDetails>
): ChildDocumentDetails => ({
  ...data,
  child: {
    ...data.child,
    dateOfBirth: LocalDate.parseNullableIso(data.child.dateOfBirth)
  },
  publishedAt: HelsinkiDateTime.parseNullableIso(data.publishedAt),
  template: {
    ...data.template,
    validity: DateRange.parseJson(data.template.validity)
  }
})

export function getChildDocumentSummaries(
  childId: UUID
): Promise<ChildDocumentCitizenSummary[]> {
  return client
    .get<JsonOf<ChildDocumentCitizenSummary[]>>(`/citizen/child-documents`, {
      params: { childId }
    })
    .then(({ data }) => data.map(deserializeChildDocumentSummary))
}

export function getChildDocumentDetails(
  id: UUID
): Promise<ChildDocumentDetails> {
  return client
    .get<JsonOf<ChildDocumentDetails>>(`/citizen/child-documents/${id}`)
    .then((res) => deserializeChildDocumentDetails(res.data))
}

export async function markChildDocumentRead(id: UUID): Promise<void> {
  await client.put(`/citizen/child-documents/${id}/read`)
}

export function getUnreadChildDocumentsCount(): Promise<Record<UUID, number>> {
  return client
    .get<JsonOf<Record<UUID, number>>>(`/citizen/child-documents/unread-count`)
    .then((res) => res.data)
}
