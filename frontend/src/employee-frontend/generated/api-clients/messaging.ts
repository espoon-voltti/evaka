// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AuthorizedMessageAccount } from 'lib-common/generated/api-types/messaging'
import { AxiosHeaders } from 'axios'
import { CreateMessageResponse } from 'lib-common/generated/api-types/messaging'
import { DraftContent } from 'lib-common/generated/api-types/messaging'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { MessageReceiversResponse } from 'lib-common/generated/api-types/messaging'
import { MessageThread } from 'lib-common/generated/api-types/messaging'
import { PagedMessageCopies } from 'lib-common/generated/api-types/messaging'
import { PagedMessageThreads } from 'lib-common/generated/api-types/messaging'
import { PagedSentMessages } from 'lib-common/generated/api-types/messaging'
import { PostMessageBody } from 'lib-common/generated/api-types/messaging'
import { PostMessagePreflightBody } from 'lib-common/generated/api-types/messaging'
import { PostMessagePreflightResponse } from 'lib-common/generated/api-types/messaging'
import { ReplyToMessageBody } from 'lib-common/generated/api-types/messaging'
import { ThreadByApplicationResponse } from 'lib-common/generated/api-types/messaging'
import { ThreadReply } from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'
import { UnreadCountByAccount } from 'lib-common/generated/api-types/messaging'
import { UpdatableDraftContent } from 'lib-common/generated/api-types/messaging'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonDraftContent } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonMessageThread } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonPagedMessageCopies } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonPagedMessageThreads } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonPagedSentMessages } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonThreadByApplicationResponse } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonThreadReply } from 'lib-common/generated/api-types/messaging'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.messaging.MessageController.archiveThread
*/
export async function archiveThread(
  request: {
    accountId: UUID,
    threadId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/messages/${request.accountId}/threads/${request.threadId}/archive`.toString(),
    method: 'PUT',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.createMessage
*/
export async function createMessage(
  request: {
    accountId: UUID,
    body: PostMessageBody
  },
  headers?: AxiosHeaders
): Promise<CreateMessageResponse> {
  const { data: json } = await client.request<JsonOf<CreateMessageResponse>>({
    url: uri`/employee/messages/${request.accountId}`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<PostMessageBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.createMessagePreflightCheck
*/
export async function createMessagePreflightCheck(
  request: {
    accountId: UUID,
    body: PostMessagePreflightBody
  },
  headers?: AxiosHeaders
): Promise<PostMessagePreflightResponse> {
  const { data: json } = await client.request<JsonOf<PostMessagePreflightResponse>>({
    url: uri`/employee/messages/${request.accountId}/preflight-check`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<PostMessagePreflightBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.deleteDraftMessage
*/
export async function deleteDraftMessage(
  request: {
    accountId: UUID,
    draftId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/messages/${request.accountId}/drafts/${request.draftId}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getAccountsByUser
*/
export async function getAccountsByUser(
  headers?: AxiosHeaders
): Promise<AuthorizedMessageAccount[]> {
  const { data: json } = await client.request<JsonOf<AuthorizedMessageAccount[]>>({
    url: uri`/employee/messages/my-accounts`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getArchivedMessages
*/
export async function getArchivedMessages(
  request: {
    accountId: UUID,
    page: number
  },
  headers?: AxiosHeaders
): Promise<PagedMessageThreads> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedMessageThreads>>({
    url: uri`/employee/messages/${request.accountId}/archived`.toString(),
    method: 'GET',
    headers,
    params
  })
  return deserializeJsonPagedMessageThreads(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getDraftMessages
*/
export async function getDraftMessages(
  request: {
    accountId: UUID
  },
  headers?: AxiosHeaders
): Promise<DraftContent[]> {
  const { data: json } = await client.request<JsonOf<DraftContent[]>>({
    url: uri`/employee/messages/${request.accountId}/drafts`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonDraftContent(e))
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getMessageCopies
*/
export async function getMessageCopies(
  request: {
    accountId: UUID,
    page: number
  },
  headers?: AxiosHeaders
): Promise<PagedMessageCopies> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedMessageCopies>>({
    url: uri`/employee/messages/${request.accountId}/copies`.toString(),
    method: 'GET',
    headers,
    params
  })
  return deserializeJsonPagedMessageCopies(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getReceivedMessages
*/
export async function getReceivedMessages(
  request: {
    accountId: UUID,
    page: number
  },
  headers?: AxiosHeaders
): Promise<PagedMessageThreads> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedMessageThreads>>({
    url: uri`/employee/messages/${request.accountId}/received`.toString(),
    method: 'GET',
    headers,
    params
  })
  return deserializeJsonPagedMessageThreads(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getReceiversForNewMessage
*/
export async function getReceiversForNewMessage(
  headers?: AxiosHeaders
): Promise<MessageReceiversResponse[]> {
  const { data: json } = await client.request<JsonOf<MessageReceiversResponse[]>>({
    url: uri`/employee/messages/receivers`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getSentMessages
*/
export async function getSentMessages(
  request: {
    accountId: UUID,
    page: number
  },
  headers?: AxiosHeaders
): Promise<PagedSentMessages> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedSentMessages>>({
    url: uri`/employee/messages/${request.accountId}/sent`.toString(),
    method: 'GET',
    headers,
    params
  })
  return deserializeJsonPagedSentMessages(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getThread
*/
export async function getThread(
  request: {
    accountId: UUID,
    threadId: UUID
  },
  headers?: AxiosHeaders
): Promise<MessageThread> {
  const { data: json } = await client.request<JsonOf<MessageThread>>({
    url: uri`/employee/messages/${request.accountId}/thread/${request.threadId}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonMessageThread(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getThreadByApplicationId
*/
export async function getThreadByApplicationId(
  request: {
    applicationId: UUID
  },
  headers?: AxiosHeaders
): Promise<ThreadByApplicationResponse> {
  const { data: json } = await client.request<JsonOf<ThreadByApplicationResponse>>({
    url: uri`/employee/messages/application/${request.applicationId}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonThreadByApplicationResponse(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getUnreadMessages
*/
export async function getUnreadMessages(
  headers?: AxiosHeaders
): Promise<UnreadCountByAccount[]> {
  const { data: json } = await client.request<JsonOf<UnreadCountByAccount[]>>({
    url: uri`/employee/messages/unread`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.initDraftMessage
*/
export async function initDraftMessage(
  request: {
    accountId: UUID
  },
  headers?: AxiosHeaders
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee/messages/${request.accountId}/drafts`.toString(),
    method: 'POST',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.markThreadRead
*/
export async function markThreadRead(
  request: {
    accountId: UUID,
    threadId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/messages/${request.accountId}/threads/${request.threadId}/read`.toString(),
    method: 'PUT',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.replyToThread
*/
export async function replyToThread(
  request: {
    accountId: UUID,
    messageId: UUID,
    body: ReplyToMessageBody
  },
  headers?: AxiosHeaders
): Promise<ThreadReply> {
  const { data: json } = await client.request<JsonOf<ThreadReply>>({
    url: uri`/employee/messages/${request.accountId}/${request.messageId}/reply`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<ReplyToMessageBody>
  })
  return deserializeJsonThreadReply(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.updateDraftMessage
*/
export async function updateDraftMessage(
  request: {
    accountId: UUID,
    draftId: UUID,
    body: UpdatableDraftContent
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/messages/${request.accountId}/drafts/${request.draftId}`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<UpdatableDraftContent>
  })
  return json
}
