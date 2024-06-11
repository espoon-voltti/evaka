// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { CurrentNotificationResponse } from 'lib-common/generated/api-types/systemnotifications'
import { JsonOf } from 'lib-common/json'
import { client } from '../../client'
import { deserializeJsonCurrentNotificationResponse } from 'lib-common/generated/api-types/systemnotifications'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.getCurrentSystemNotificationEmployeeMobile
*/
export async function getCurrentSystemNotificationEmployeeMobile(): Promise<CurrentNotificationResponse> {
  const { data: json } = await client.request<JsonOf<CurrentNotificationResponse>>({
    url: uri`/employee-mobile/system-notifications/current`.toString(),
    method: 'GET'
  })
  return deserializeJsonCurrentNotificationResponse(json)
}
