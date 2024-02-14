// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import LocalDate from 'lib-common/local-date'
import { JsonOf } from 'lib-common/json'
import { Timeline } from 'lib-common/generated/api-types/timeline'
import { UUID } from 'lib-common/types'
import { client } from '../../client'
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
  const { data: json } = await client.request<JsonOf<Timeline>>({
    url: uri`/timeline`.toString(),
    method: 'GET',
    params: {
      personId: request.personId,
      from: request.from.formatIso(),
      to: request.to.formatIso()
    }
  })
  return deserializeJsonTimeline(json)
}
