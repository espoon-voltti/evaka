// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { SubscribeRequest } from 'lib-common/generated/api-types/citizenwebpush'
import type { SubscribeResponse } from 'lib-common/generated/api-types/citizenwebpush'
import type { VapidKeyResponse } from 'lib-common/generated/api-types/citizenwebpush'
import { client } from '../../api-client'
import { createUrlSearchParams } from 'lib-common/api'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.citizenwebpush.CitizenWebPushController.deleteSubscription
*/
export async function deleteSubscription(
  request: {
    endpoint: string
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['endpoint', request.endpoint.toString()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/web-push/subscription`.toString(),
    method: 'DELETE',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.citizenwebpush.CitizenWebPushController.postTest
*/
export async function postTest(): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/web-push/test`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.citizenwebpush.CitizenWebPushController.putSubscription
*/
export async function putSubscription(
  request: {
    body: SubscribeRequest
  }
): Promise<SubscribeResponse> {
  const { data: json } = await client.request<JsonOf<SubscribeResponse>>({
    url: uri`/citizen/web-push/subscription`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<SubscribeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.citizenwebpush.CitizenWebPushController.vapidKey
*/
export async function vapidKey(): Promise<VapidKeyResponse> {
  const { data: json } = await client.request<JsonOf<VapidKeyResponse>>({
    url: uri`/citizen/web-push/vapid-key`.toString(),
    method: 'GET'
  })
  return json
}
