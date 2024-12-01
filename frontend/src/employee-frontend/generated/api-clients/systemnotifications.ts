// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AxiosHeaders } from 'axios'
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/system-notifications/${request.targetGroup}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.getAllSystemNotifications
*/
export async function getAllSystemNotifications(
  headers?: AxiosHeaders
): Promise<SystemNotification[]> {
  const { data: json } = await client.request<JsonOf<SystemNotification[]>>({
    url: uri`/employee/system-notifications`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonSystemNotification(e))
}


/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.putSystemNotification
*/
export async function putSystemNotification(
  request: {
    body: SystemNotification
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/system-notifications`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<SystemNotification>
  })
  return json
}
