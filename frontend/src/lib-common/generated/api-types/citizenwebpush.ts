// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

/**
* Generated from fi.espoo.evaka.citizenwebpush.CitizenPushCategory
*/
export type CitizenPushCategory =
  | 'URGENT_MESSAGE'
  | 'MESSAGE'
  | 'BULLETIN'

/**
* Generated from fi.espoo.evaka.citizenwebpush.CitizenWebPushController.SubscribeRequest
*/
export interface SubscribeRequest {
  authSecret: number[]
  ecdhKey: number[]
  enabledCategories: CitizenPushCategory[]
  endpoint: string
  userAgent: string | null
}

/**
* Generated from fi.espoo.evaka.citizenwebpush.CitizenWebPushController.SubscribeResponse
*/
export interface SubscribeResponse {
  sentTest: boolean
}

/**
* Generated from fi.espoo.evaka.citizenwebpush.CitizenWebPushController.VapidKeyResponse
*/
export interface VapidKeyResponse {
  publicKey: string
}
