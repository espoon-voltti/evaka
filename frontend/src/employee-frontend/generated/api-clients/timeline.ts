// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { JsonOf } from 'lib-common/json'
import { Timeline } from 'lib-common/generated/api-types/timeline'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonTimeline } from 'lib-common/generated/api-types/timeline'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.timeline.TimelineController.getTimeline
*/
export async function getTimeline(
  request: {
    personId: UUID,
    from: LocalDate,
    to: LocalDate
  }
): Promise<Timeline> {
  const params = createUrlSearchParams(
    ['personId', request.personId],
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<Timeline>>({
    url: uri`/employee/timeline`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonTimeline(json)
}
