// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from 'citizen-frontend/api-client'
import { Failure, Result, Success } from 'lib-common/api'
import {
  CitizenGetVasuDocumentSummariesResponse,
  VasuDocumentEvent
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
): Promise<Result<CitizenGetVasuDocumentSummariesResponse>> {
  return client
    .get<JsonOf<CitizenGetVasuDocumentSummariesResponse>>(
      `/citizen/vasu/children/${childId}/vasu-summaries`
    )
    .then((res) =>
      Success.of({
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
      })
    )
    .catch((e) => Failure.fromError(e))
}
