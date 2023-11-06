// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import HelsinkiDateTime from '../../helsinki-date-time'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.webpush.PushNotificationCategory
*/
export const pushNotificationCategories = [
  'RECEIVED_MESSAGE'
] as const

export type PushNotificationCategory = typeof pushNotificationCategories[number]

/**
* Generated from fi.espoo.evaka.webpush.WebPushController.PushSettings
*/
export interface PushSettings {
  categories: PushNotificationCategory[]
  groups: UUID[]
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
