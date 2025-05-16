// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CurrentNotificationResponseEmployee } from 'lib-common/generated/api-types/systemnotifications'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { SystemNotificationCitizens } from 'lib-common/generated/api-types/systemnotifications'
import type { SystemNotificationEmployees } from 'lib-common/generated/api-types/systemnotifications'
import type { SystemNotificationTargetGroup } from 'lib-common/generated/api-types/systemnotifications'
import type { SystemNotificationsResponse } from 'lib-common/generated/api-types/systemnotifications'
import { client } from '../../api/client'
import { deserializeJsonCurrentNotificationResponseEmployee } from 'lib-common/generated/api-types/systemnotifications'
import { deserializeJsonSystemNotificationsResponse } from 'lib-common/generated/api-types/systemnotifications'
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
export async function getAllSystemNotifications(): Promise<SystemNotificationsResponse> {
  const { data: json } = await client.request<JsonOf<SystemNotificationsResponse>>({
    url: uri`/employee/system-notifications`.toString(),
    method: 'GET'
  })
  return deserializeJsonSystemNotificationsResponse(json)
}


/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.getCurrentSystemNotificationEmployee
*/
export async function getCurrentSystemNotificationEmployee(): Promise<CurrentNotificationResponseEmployee> {
  const { data: json } = await client.request<JsonOf<CurrentNotificationResponseEmployee>>({
    url: uri`/employee/public/system-notifications/current`.toString(),
    method: 'GET'
  })
  return deserializeJsonCurrentNotificationResponseEmployee(json)
}


/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.putSystemNotificationCitizens
*/
export async function putSystemNotificationCitizens(
  request: {
    body: SystemNotificationCitizens
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/system-notifications/citizens`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<SystemNotificationCitizens>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.putSystemNotificationEmployees
*/
export async function putSystemNotificationEmployees(
  request: {
    body: SystemNotificationEmployees
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/system-notifications/employees`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<SystemNotificationEmployees>
  })
  return json
}
