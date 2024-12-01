// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AxiosHeaders } from 'axios'
import { CitizenMessageBody } from 'lib-common/generated/api-types/messaging'
import { GetReceiversResponse } from 'lib-common/generated/api-types/messaging'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { MyAccountResponse } from 'lib-common/generated/api-types/messaging'
import { PagedCitizenMessageThreads } from 'lib-common/generated/api-types/messaging'
import { ReplyToMessageBody } from 'lib-common/generated/api-types/messaging'
import { ThreadReply } from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'
import { client } from '../../api-client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonPagedCitizenMessageThreads } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonThreadReply } from 'lib-common/generated/api-types/messaging'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.archiveThread
*/
export async function archiveThread(
  request: {
    threadId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/messages/threads/${request.threadId}/archive`.toString(),
    method: 'PUT',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getMyAccount
*/
export async function getMyAccount(
  headers?: AxiosHeaders
): Promise<MyAccountResponse> {
  const { data: json } = await client.request<JsonOf<MyAccountResponse>>({
    url: uri`/citizen/messages/my-account`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getReceivedMessages
*/
export async function getReceivedMessages(
  request: {
    page: number
  },
  headers?: AxiosHeaders
): Promise<PagedCitizenMessageThreads> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedCitizenMessageThreads>>({
    url: uri`/citizen/messages/received`.toString(),
    method: 'GET',
    headers,
    params
  })
  return deserializeJsonPagedCitizenMessageThreads(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getReceivers
*/
export async function getReceivers(
  headers?: AxiosHeaders
): Promise<GetReceiversResponse> {
  const { data: json } = await client.request<JsonOf<GetReceiversResponse>>({
    url: uri`/citizen/messages/receivers`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.getUnreadMessages
*/
export async function getUnreadMessages(
  headers?: AxiosHeaders
): Promise<number> {
  const { data: json } = await client.request<JsonOf<number>>({
    url: uri`/citizen/messages/unread-count`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.markThreadRead
*/
export async function markThreadRead(
  request: {
    threadId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/messages/threads/${request.threadId}/read`.toString(),
    method: 'PUT',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.newMessage
*/
export async function newMessage(
  request: {
    body: CitizenMessageBody
  },
  headers?: AxiosHeaders
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/citizen/messages`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<CitizenMessageBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageControllerCitizen.replyToThread
*/
export async function replyToThread(
  request: {
    messageId: UUID,
    body: ReplyToMessageBody
  },
  headers?: AxiosHeaders
): Promise<ThreadReply> {
  const { data: json } = await client.request<JsonOf<ThreadReply>>({
    url: uri`/citizen/messages/${request.messageId}/reply`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<ReplyToMessageBody>
  })
  return deserializeJsonThreadReply(json)
}
