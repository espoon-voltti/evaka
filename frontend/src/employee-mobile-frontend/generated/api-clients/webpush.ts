// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AxiosHeaders } from 'axios'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PushSettings } from 'lib-common/generated/api-types/webpush'
import { WebPushSubscription } from 'lib-common/generated/api-types/webpush'
import { client } from '../../client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.webpush.WebPushController.getPushSettings
*/
export async function getPushSettings(
  headers?: AxiosHeaders
): Promise<PushSettings> {
  const { data: json } = await client.request<JsonOf<PushSettings>>({
    url: uri`/employee-mobile/push-settings`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.webpush.WebPushController.setPushSettings
*/
export async function setPushSettings(
  request: {
    body: PushSettings
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/push-settings`.toString(),
    method: 'PUT',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/push-subscription`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<WebPushSubscription>
  })
  return json
}
