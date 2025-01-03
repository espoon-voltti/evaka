// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import { GroupId } from './shared'
import { JsonOf } from '../../json'

/**
* Generated from fi.espoo.evaka.webpush.PushNotificationCategory
*/
export const pushNotificationCategories = [
  'RECEIVED_MESSAGE',
  'NEW_ABSENCE',
  'CALENDAR_EVENT_RESERVATION'
] as const

export type PushNotificationCategory = typeof pushNotificationCategories[number]

/**
* Generated from fi.espoo.evaka.webpush.WebPushController.PushSettings
*/
export interface PushSettings {
  categories: PushNotificationCategory[]
  groups: GroupId[]
}

/**
* Generated from fi.espoo.evaka.webpush.WebPushSubscription
*/
export interface WebPushSubscription {
  authSecret: number[]
  ecdhKey: number[]
  endpoint: string
  expires: HelsinkiDateTime | null
}


export function deserializeJsonWebPushSubscription(json: JsonOf<WebPushSubscription>): WebPushSubscription {
  return {
    ...json,
    expires: (json.expires != null) ? HelsinkiDateTime.parseIso(json.expires) : null
  }
}
