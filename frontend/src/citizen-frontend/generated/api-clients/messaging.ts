// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CitizenMessageBody } from 'lib-common/generated/api-types/messaging'
import type { GetRecipientsResponse } from 'lib-common/generated/api-types/messaging'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { MessageId } from 'lib-common/generated/api-types/shared'
import type { MessageThreadId } from 'lib-common/generated/api-types/shared'
import type { MyAccountResponse } from 'lib-common/generated/api-types/messaging'
import type { PagedCitizenMessageThreads } from 'lib-common/generated/api-types/messaging'
import type { ReplyToMessageBody } from 'lib-common/generated/api-types/messaging'
import type { ThreadReply } from 'lib-common/generated/api-types/messaging'
import { client } from '../../api-client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonGetRecipientsResponse } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonPagedCitizenMessageThreads } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonThreadReply } from 'lib-common/generated/api-types/messaging'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.archiveThread
*/
export async function archiveThread(
  request: {
    threadId: MessageThreadId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/messages/threads/${request.threadId}/archive`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getMyAccount
*/
export async function getMyAccount(): Promise<MyAccountResponse> {
  const { data: json } = await client.request<JsonOf<MyAccountResponse>>({
    url: uri`/citizen/messages/my-account`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getReceivedMessages
*/
export async function getReceivedMessages(
  request: {
    page: number
  }
): Promise<PagedCitizenMessageThreads> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedCitizenMessageThreads>>({
    url: uri`/citizen/messages/received`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonPagedCitizenMessageThreads(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getRecipients
*/
export async function getRecipients(): Promise<GetRecipientsResponse> {
  const { data: json } = await client.request<JsonOf<GetRecipientsResponse>>({
    url: uri`/citizen/messages/recipients`.toString(),
    method: 'GET'
  })
  return deserializeJsonGetRecipientsResponse(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getUnreadMessages
*/
export async function getUnreadMessages(): Promise<number> {
  const { data: json } = await client.request<JsonOf<number>>({
    url: uri`/citizen/messages/unread-count`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.markLastReceivedMessageInThreadUnread
*/
export async function markLastReceivedMessageInThreadUnread(
  request: {
    threadId: MessageThreadId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/messages/threads/${request.threadId}/last-received-message/read`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.markThreadRead
*/
export async function markThreadRead(
  request: {
    threadId: MessageThreadId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/messages/threads/${request.threadId}/read`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.newMessage
*/
export async function newMessage(
  request: {
    body: CitizenMessageBody
  }
): Promise<MessageThreadId> {
  const { data: json } = await client.request<JsonOf<MessageThreadId>>({
    url: uri`/citizen/messages`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CitizenMessageBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.replyToThread
*/
export async function replyToThread(
  request: {
    messageId: MessageId,
    body: ReplyToMessageBody
  }
): Promise<ThreadReply> {
  const { data: json } = await client.request<JsonOf<ThreadReply>>({
    url: uri`/citizen/messages/${request.messageId}/reply`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ReplyToMessageBody>
  })
  return deserializeJsonThreadReply(json)
}
