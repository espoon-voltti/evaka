// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../api-client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesCitizenController.dismissDailyServiceTimeNotification
*/
export async function dismissDailyServiceTimeNotification(
  request: {
    body: UUID[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/daily-service-time-notifications/dismiss`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesCitizenController.getDailyServiceTimeNotifications
*/
export async function getDailyServiceTimeNotifications(): Promise<UUID[]> {
  const { data: json } = await client.request<JsonOf<UUID[]>>({
    url: uri`/citizen/daily-service-time-notifications`.toString(),
    method: 'GET'
  })
  return json
}
