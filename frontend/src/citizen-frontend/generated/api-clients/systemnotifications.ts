// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { CurrentNotificationResponseCitizen } from 'lib-common/generated/api-types/systemnotifications'
import { JsonOf } from 'lib-common/json'
import { client } from '../../api-client'
import { deserializeJsonCurrentNotificationResponseCitizen } from 'lib-common/generated/api-types/systemnotifications'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.getCurrentSystemNotificationCitizen
*/
export async function getCurrentSystemNotificationCitizen(): Promise<CurrentNotificationResponseCitizen> {
  const { data: json } = await client.request<JsonOf<CurrentNotificationResponseCitizen>>({
    url: uri`/citizen/public/system-notifications/current`.toString(),
    method: 'GET'
  })
  return deserializeJsonCurrentNotificationResponseCitizen(json)
}
