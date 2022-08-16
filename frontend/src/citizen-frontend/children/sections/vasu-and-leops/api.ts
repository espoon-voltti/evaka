// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'citizen-frontend/api-client'
import { Failure, Result, Success } from 'lib-common/api'
import {
  VasuDocumentEvent,
  VasuDocumentSummary
} from 'lib-common/generated/api-types/vasu'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

const mapVasuDocumentEvent = (
  e: JsonOf<VasuDocumentEvent>
): VasuDocumentEvent => ({
  ...e,
  created: HelsinkiDateTime.parseIso(e.created)
})

export async function getChildVasuSummaries(
  childId: UUID
): Promise<Result<VasuDocumentSummary[]>> {
  return client
    .get<JsonOf<VasuDocumentSummary[]>>(
      `/citizen/vasu/children/${childId}/vasu-summaries`
    )
    .then((res) =>
      Success.of(
        res.data.map(({ events, modifiedAt, publishedAt, ...rest }) => ({
          ...rest,
          events: events.map(mapVasuDocumentEvent),
          modifiedAt: HelsinkiDateTime.parseIso(modifiedAt),
          publishedAt: publishedAt
            ? HelsinkiDateTime.parseIso(publishedAt)
            : null
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getUnreadVasuDocumentsCount(): Promise<
  Result<Record<UUID, number>>
> {
  try {
    const count = await client
      .get<JsonOf<Record<UUID, number>>>(`/citizen/vasu/children/unread-count`)
      .then((res) => res.data)
    return Success.of(count)
  } catch (e) {
    return Failure.fromError(e)
  }
}
