// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PushSettings } from 'lib-common/generated/api-types/webpush'
import { WebPushSubscription } from 'lib-common/generated/api-types/webpush'
import { client } from '../../client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.webpush.WebPushController.getPushSettings
*/
export async function getPushSettings(): Promise<PushSettings> {
  const { data: json } = await client.request<JsonOf<PushSettings>>({
    url: uri`/mobile-devices/push-settings`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.webpush.WebPushController.setPushSettings
*/
export async function setPushSettings(
  request: {
    body: PushSettings
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/mobile-devices/push-settings`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<PushSettings>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.webpush.WebPushController.upsertPushSubscription
*/
export async function upsertPushSubscription(
  request: {
    body: WebPushSubscription
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/mobile-devices/push-subscription`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<WebPushSubscription>
  })
  return json
}
