// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CitizenPushSubscriptionId } from '../api-types/shared'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { MessageThreadId } from '../api-types/shared'
import type { MobileMyAccount } from '../api-types/messaging'
import type { MobileThread } from '../api-types/messaging'
import type { MobileThreadListResponse } from '../api-types/messaging'
import type { ReplyBody } from '../api-types/messaging'
import type { ThreadReply } from '../api-types/messaging'
import type { UpsertBody } from '../api-types/messaging'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonMobileThread } from '../api-types/messaging'
import { deserializeJsonMobileThreadListResponse } from '../api-types/messaging'
import { deserializeJsonThreadReply } from '../api-types/messaging'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.messaging.mobile.CitizenPushSubscriptionController.deleteSubscription
*/
export async function deleteSubscription(
  request: {
    deviceId: CitizenPushSubscriptionId
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['deviceId', request.deviceId]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen-mobile/push-subscriptions/v1`.toString(),
    method: 'DELETE',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.mobile.CitizenPushSubscriptionController.upsertSubscription
*/
export async function upsertSubscription(
  request: {
    body: UpsertBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen-mobile/push-subscriptions/v1`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<UpsertBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.mobile.MessageControllerMobile.getMyAccount
*/
export async function getMyAccount(): Promise<MobileMyAccount> {
  const { data: json } = await client.request<JsonOf<MobileMyAccount>>({
    url: uri`/citizen-mobile/messages/my-account/v1`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.mobile.MessageControllerMobile.getThread
*/
export async function getThread(
  request: {
    threadId: MessageThreadId
  }
): Promise<MobileThread> {
  const { data: json } = await client.request<JsonOf<MobileThread>>({
    url: uri`/citizen-mobile/messages/thread/${request.threadId}/v1`.toString(),
    method: 'GET'
  })
  return deserializeJsonMobileThread(json)
}


/**
* Generated from fi.espoo.evaka.messaging.mobile.MessageControllerMobile.getThreads
*/
export async function getThreads(
  request: {
    pageSize: number,
    page: number
  }
): Promise<MobileThreadListResponse> {
  const params = createUrlSearchParams(
    ['pageSize', request.pageSize.toString()],
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<MobileThreadListResponse>>({
    url: uri`/citizen-mobile/messages/threads/v1`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonMobileThreadListResponse(json)
}


/**
* Generated from fi.espoo.evaka.messaging.mobile.MessageControllerMobile.getUnreadCount
*/
export async function getUnreadCount(): Promise<number> {
  const { data: json } = await client.request<JsonOf<number>>({
    url: uri`/citizen-mobile/messages/unread-count/v1`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.mobile.MessageControllerMobile.markThreadRead
*/
export async function markThreadRead(
  request: {
    threadId: MessageThreadId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen-mobile/messages/thread/${request.threadId}/mark-read/v1`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.mobile.MessageControllerMobile.replyToThread
*/
export async function replyToThread(
  request: {
    threadId: MessageThreadId,
    body: ReplyBody
  }
): Promise<ThreadReply> {
  const { data: json } = await client.request<JsonOf<ThreadReply>>({
    url: uri`/citizen-mobile/messages/thread/${request.threadId}/reply/v1`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ReplyBody>
  })
  return deserializeJsonThreadReply(json)
}
