// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { SystemNotification } from 'lib-common/generated/api-types/systemnotifications'
import { SystemNotificationTargetGroup } from 'lib-common/generated/api-types/systemnotifications'
import { client } from '../../api/client'
import { deserializeJsonSystemNotification } from 'lib-common/generated/api-types/systemnotifications'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.deleteSystemNotification
*/
export async function deleteSystemNotification(
  request: {
    targetGroup: SystemNotificationTargetGroup
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/system-notifications/${request.targetGroup}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.getAllSystemNotifications
*/
export async function getAllSystemNotifications(): Promise<SystemNotification[]> {
  const { data: json } = await client.request<JsonOf<SystemNotification[]>>({
    url: uri`/employee/system-notifications`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonSystemNotification(e))
}


/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.putSystemNotification
*/
export async function putSystemNotification(
  request: {
    body: SystemNotification
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/system-notifications`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<SystemNotification>
  })
  return json
}
