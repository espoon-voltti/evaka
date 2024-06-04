// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import { JsonOf } from '../../json'

/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationsController.CurrentNotificationResponse
*/
export interface CurrentNotificationResponse {
  notification: SystemNotification | null
}

/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotification
*/
export interface SystemNotification {
  targetGroup: SystemNotificationTargetGroup
  text: string
  validTo: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.systemnotifications.SystemNotificationTargetGroup
*/
export type SystemNotificationTargetGroup =
  | 'CITIZENS'
  | 'EMPLOYEES'


export function deserializeJsonCurrentNotificationResponse(json: JsonOf<CurrentNotificationResponse>): CurrentNotificationResponse {
  return {
    ...json,
    notification: (json.notification != null) ? deserializeJsonSystemNotification(json.notification) : null
  }
}


export function deserializeJsonSystemNotification(json: JsonOf<SystemNotification>): SystemNotification {
  return {
    ...json,
    validTo: HelsinkiDateTime.parseIso(json.validTo)
  }
}
