// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { CurrentNotificationResponseEmployee } from 'lib-common/generated/api-types/systemnotifications'
import { JsonOf } from 'lib-common/json'
import { client } from '../../client'
import { deserializeJsonCurrentNotificationResponseEmployee } from 'lib-common/generated/api-types/systemnotifications'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.getCurrentSystemNotificationEmployeeMobile
*/
export async function getCurrentSystemNotificationEmployeeMobile(): Promise<CurrentNotificationResponseEmployee> {
  const { data: json } = await client.request<JsonOf<CurrentNotificationResponseEmployee>>({
    url: uri`/employee-mobile/system-notifications/current`.toString(),
    method: 'GET'
  })
  return deserializeJsonCurrentNotificationResponseEmployee(json)
}
