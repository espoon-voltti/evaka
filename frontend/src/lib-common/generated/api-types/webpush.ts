// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CitizenPushSubscriptionId } from './shared'
import type { DeviceClass } from './user'
import type { GroupId } from './shared'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'

/**
* Generated from evaka.core.webpush.CitizenPushDevice
*/
export interface CitizenPushDevice {
  agentName: string
  createdAt: HelsinkiDateTime
  deviceClass: DeviceClass
  id: CitizenPushSubscriptionId
  installed: boolean
  lastSentAt: HelsinkiDateTime | null
  operatingSystemName: string
}

/**
* Generated from evaka.core.webpush.CitizenWebPushController.CitizenPushSettings
*/
export interface CitizenPushSettings {
  applicationServerKey: string | null
  devices: CitizenPushDevice[]
}

/**
* Generated from evaka.core.webpush.CitizenWebPushController.NewCitizenPushSubscription
*/
export interface NewCitizenPushSubscription {
  installed: boolean
  subscription: WebPushSubscription
}

/**
* Generated from evaka.core.webpush.PushNotificationCategory
*/
export const pushNotificationCategories = [
  'RECEIVED_MESSAGE',
  'NEW_ABSENCE',
  'CALENDAR_EVENT_RESERVATION'
] as const

export type PushNotificationCategory = typeof pushNotificationCategories[number]

/**
* Generated from evaka.core.webpush.WebPushController.PushSettings
*/
export interface PushSettings {
  categories: PushNotificationCategory[]
  groups: GroupId[]
}

/**
* Generated from evaka.core.webpush.CitizenWebPushController.PushSubscriptionCheckRequest
*/
export interface PushSubscriptionCheckRequest {
  endpoint: string
}

/**
* Generated from evaka.core.webpush.CitizenWebPushController.PushSubscriptionCheckResponse
*/
export interface PushSubscriptionCheckResponse {
  deviceId: CitizenPushSubscriptionId | null
}

/**
* Generated from evaka.core.webpush.WebPushSubscription
*/
export interface WebPushSubscription {
  authSecret: number[]
  ecdhKey: number[]
  endpoint: string
  expires: HelsinkiDateTime | null
}


export function deserializeJsonCitizenPushDevice(json: JsonOf<CitizenPushDevice>): CitizenPushDevice {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    lastSentAt: (json.lastSentAt != null) ? HelsinkiDateTime.parseIso(json.lastSentAt) : null
  }
}


export function deserializeJsonCitizenPushSettings(json: JsonOf<CitizenPushSettings>): CitizenPushSettings {
  return {
    ...json,
    devices: json.devices.map(e => deserializeJsonCitizenPushDevice(e))
  }
}


export function deserializeJsonNewCitizenPushSubscription(json: JsonOf<NewCitizenPushSubscription>): NewCitizenPushSubscription {
  return {
    ...json,
    subscription: deserializeJsonWebPushSubscription(json.subscription)
  }
}


export function deserializeJsonWebPushSubscription(json: JsonOf<WebPushSubscription>): WebPushSubscription {
  return {
    ...json,
    expires: (json.expires != null) ? HelsinkiDateTime.parseIso(json.expires) : null
  }
}
