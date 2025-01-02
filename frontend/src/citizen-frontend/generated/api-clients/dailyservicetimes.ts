// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { DailyServiceTimeNotificationId } from 'lib-common/generated/api-types/shared'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { client } from '../../api-client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesCitizenController.dismissDailyServiceTimeNotification
*/
export async function dismissDailyServiceTimeNotification(
  request: {
    body: DailyServiceTimeNotificationId[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/daily-service-time-notifications/dismiss`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DailyServiceTimeNotificationId[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.dailyservicetimes.DailyServiceTimesCitizenController.getDailyServiceTimeNotifications
*/
export async function getDailyServiceTimeNotifications(): Promise<DailyServiceTimeNotificationId[]> {
  const { data: json } = await client.request<JsonOf<DailyServiceTimeNotificationId[]>>({
    url: uri`/citizen/daily-service-time-notifications`.toString(),
    method: 'GET'
  })
  return json
}
