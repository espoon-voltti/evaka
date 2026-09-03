// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CitizenPushDevice } from 'lib-common/generated/api-types/webpush'
import type { CitizenPushSettings } from 'lib-common/generated/api-types/webpush'
import type { CitizenPushSubscriptionId } from 'lib-common/generated/api-types/shared'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { NewCitizenPushSubscription } from 'lib-common/generated/api-types/webpush'
import type { PushSubscriptionCheckRequest } from 'lib-common/generated/api-types/webpush'
import type { PushSubscriptionCheckResponse } from 'lib-common/generated/api-types/webpush'
import type { PushTestRequest } from 'lib-common/generated/api-types/webpush'
import { client } from '../../api-client'
import { deserializeJsonCitizenPushDevice } from 'lib-common/generated/api-types/webpush'
import { deserializeJsonCitizenPushSettings } from 'lib-common/generated/api-types/webpush'
import { uri } from 'lib-common/uri'


/**
* Generated from evaka.core.webpush.CitizenWebPushController.addPushSubscription
*/
export async function addPushSubscription(
  request: {
    body: NewCitizenPushSubscription
  }
): Promise<CitizenPushDevice> {
  const { data: json } = await client.request<JsonOf<CitizenPushDevice>>({
    url: uri`/citizen/push-subscription`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<NewCitizenPushSubscription>
  })
  return deserializeJsonCitizenPushDevice(json)
}


/**
* Generated from evaka.core.webpush.CitizenWebPushController.checkPushSubscription
*/
export async function checkPushSubscription(
  request: {
    body: PushSubscriptionCheckRequest
  }
): Promise<PushSubscriptionCheckResponse> {
  const { data: json } = await client.request<JsonOf<PushSubscriptionCheckResponse>>({
    url: uri`/citizen/push-subscription/check`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PushSubscriptionCheckRequest>
  })
  return json
}


/**
* Generated from evaka.core.webpush.CitizenWebPushController.deletePushDevice
*/
export async function deletePushDevice(
  request: {
    id: CitizenPushSubscriptionId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/push-devices/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from evaka.core.webpush.CitizenWebPushController.getPushSettings
*/
export async function getPushSettings(): Promise<CitizenPushSettings> {
  const { data: json } = await client.request<JsonOf<CitizenPushSettings>>({
    url: uri`/citizen/push-settings`.toString(),
    method: 'GET'
  })
  return deserializeJsonCitizenPushSettings(json)
}


/**
* Generated from evaka.core.webpush.CitizenWebPushController.sendTestPushNotification
*/
export async function sendTestPushNotification(
  request: {
    body: PushTestRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/push-test`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PushTestRequest>
  })
  return json
}
