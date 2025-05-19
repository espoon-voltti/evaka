// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import type { AuthorizedMessageAccount } from 'lib-common/generated/api-types/messaging'
import type { CreateMessageResponse } from 'lib-common/generated/api-types/messaging'
import type { DraftContent } from 'lib-common/generated/api-types/messaging'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { MessageAccountId } from 'lib-common/generated/api-types/shared'
import type { MessageDraftId } from 'lib-common/generated/api-types/shared'
import type { MessageId } from 'lib-common/generated/api-types/shared'
import type { MessageThread } from 'lib-common/generated/api-types/messaging'
import type { MessageThreadFolder } from 'lib-common/generated/api-types/messaging'
import type { MessageThreadFolderId } from 'lib-common/generated/api-types/shared'
import type { MessageThreadId } from 'lib-common/generated/api-types/shared'
import type { PagedMessageCopies } from 'lib-common/generated/api-types/messaging'
import type { PagedMessageThreads } from 'lib-common/generated/api-types/messaging'
import type { PagedSentMessages } from 'lib-common/generated/api-types/messaging'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { PostMessageBody } from 'lib-common/generated/api-types/messaging'
import type { PostMessagePreflightBody } from 'lib-common/generated/api-types/messaging'
import type { PostMessagePreflightResponse } from 'lib-common/generated/api-types/messaging'
import type { ReplyToMessageBody } from 'lib-common/generated/api-types/messaging'
import type { SelectableRecipientsResponse } from 'lib-common/generated/api-types/messaging'
import type { ThreadByApplicationResponse } from 'lib-common/generated/api-types/messaging'
import type { ThreadReply } from 'lib-common/generated/api-types/messaging'
import type { UnreadCountByAccount } from 'lib-common/generated/api-types/messaging'
import type { UpdatableDraftContent } from 'lib-common/generated/api-types/messaging'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonDraftContent } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonMessageThread } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonPagedMessageCopies } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonPagedMessageThreads } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonPagedSentMessages } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonSelectableRecipientsResponse } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonThreadByApplicationResponse } from 'lib-common/generated/api-types/messaging'
import { deserializeJsonThreadReply } from 'lib-common/generated/api-types/messaging'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.messaging.MessageController.archiveThread
*/
export async function archiveThread(
  request: {
    accountId: MessageAccountId,
    threadId: MessageThreadId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/messages/${request.accountId}/threads/${request.threadId}/archive`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.createMessage
*/
export async function createMessage(
  request: {
    accountId: MessageAccountId,
    initialFolder?: MessageThreadFolderId | null,
    body: PostMessageBody
  }
): Promise<CreateMessageResponse> {
  const params = createUrlSearchParams(
    ['initialFolder', request.initialFolder]
  )
  const { data: json } = await client.request<JsonOf<CreateMessageResponse>>({
    url: uri`/employee/messages/${request.accountId}`.toString(),
    method: 'POST',
    params,
    data: request.body satisfies JsonCompatible<PostMessageBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.createMessagePreflightCheck
*/
export async function createMessagePreflightCheck(
  request: {
    accountId: MessageAccountId,
    body: PostMessagePreflightBody
  }
): Promise<PostMessagePreflightResponse> {
  const { data: json } = await client.request<JsonOf<PostMessagePreflightResponse>>({
    url: uri`/employee/messages/${request.accountId}/preflight-check`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PostMessagePreflightBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.deleteDraftMessage
*/
export async function deleteDraftMessage(
  request: {
    accountId: MessageAccountId,
    draftId: MessageDraftId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/messages/${request.accountId}/drafts/${request.draftId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getAccountsByUser
*/
export async function getAccountsByUser(): Promise<AuthorizedMessageAccount[]> {
  const { data: json } = await client.request<JsonOf<AuthorizedMessageAccount[]>>({
    url: uri`/employee/messages/my-accounts`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getArchivedMessages
*/
export async function getArchivedMessages(
  request: {
    accountId: MessageAccountId,
    page: number
  }
): Promise<PagedMessageThreads> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedMessageThreads>>({
    url: uri`/employee/messages/${request.accountId}/archived`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonPagedMessageThreads(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getDraftMessages
*/
export async function getDraftMessages(
  request: {
    accountId: MessageAccountId
  }
): Promise<DraftContent[]> {
  const { data: json } = await client.request<JsonOf<DraftContent[]>>({
    url: uri`/employee/messages/${request.accountId}/drafts`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonDraftContent(e))
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getFinanceMessagesWithPerson
*/
export async function getFinanceMessagesWithPerson(
  request: {
    personId: PersonId
  }
): Promise<MessageThread[]> {
  const { data: json } = await client.request<JsonOf<MessageThread[]>>({
    url: uri`/employee/messages/finance/${request.personId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonMessageThread(e))
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getFolders
*/
export async function getFolders(): Promise<MessageThreadFolder[]> {
  const { data: json } = await client.request<JsonOf<MessageThreadFolder[]>>({
    url: uri`/employee/messages/folders`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getMessageCopies
*/
export async function getMessageCopies(
  request: {
    accountId: MessageAccountId,
    page: number
  }
): Promise<PagedMessageCopies> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedMessageCopies>>({
    url: uri`/employee/messages/${request.accountId}/copies`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonPagedMessageCopies(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getMessagesInFolder
*/
export async function getMessagesInFolder(
  request: {
    accountId: MessageAccountId,
    folderId: MessageThreadFolderId,
    page: number
  }
): Promise<PagedMessageThreads> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedMessageThreads>>({
    url: uri`/employee/messages/${request.accountId}/folders/${request.folderId}`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonPagedMessageThreads(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getReceivedMessages
*/
export async function getReceivedMessages(
  request: {
    accountId: MessageAccountId,
    page: number
  }
): Promise<PagedMessageThreads> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedMessageThreads>>({
    url: uri`/employee/messages/${request.accountId}/received`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonPagedMessageThreads(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getSelectableRecipients
*/
export async function getSelectableRecipients(): Promise<SelectableRecipientsResponse[]> {
  const { data: json } = await client.request<JsonOf<SelectableRecipientsResponse[]>>({
    url: uri`/employee/messages/selectable-recipients`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonSelectableRecipientsResponse(e))
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getSentMessages
*/
export async function getSentMessages(
  request: {
    accountId: MessageAccountId,
    page: number
  }
): Promise<PagedSentMessages> {
  const params = createUrlSearchParams(
    ['page', request.page.toString()]
  )
  const { data: json } = await client.request<JsonOf<PagedSentMessages>>({
    url: uri`/employee/messages/${request.accountId}/sent`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonPagedSentMessages(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getThread
*/
export async function getThread(
  request: {
    accountId: MessageAccountId,
    threadId: MessageThreadId
  }
): Promise<MessageThread> {
  const { data: json } = await client.request<JsonOf<MessageThread>>({
    url: uri`/employee/messages/${request.accountId}/thread/${request.threadId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonMessageThread(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getThreadByApplicationId
*/
export async function getThreadByApplicationId(
  request: {
    applicationId: ApplicationId
  }
): Promise<ThreadByApplicationResponse> {
  const { data: json } = await client.request<JsonOf<ThreadByApplicationResponse>>({
    url: uri`/employee/messages/application/${request.applicationId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonThreadByApplicationResponse(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.getUnreadMessages
*/
export async function getUnreadMessages(): Promise<UnreadCountByAccount[]> {
  const { data: json } = await client.request<JsonOf<UnreadCountByAccount[]>>({
    url: uri`/employee/messages/unread`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.initDraftMessage
*/
export async function initDraftMessage(
  request: {
    accountId: MessageAccountId
  }
): Promise<MessageDraftId> {
  const { data: json } = await client.request<JsonOf<MessageDraftId>>({
    url: uri`/employee/messages/${request.accountId}/drafts`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.markLastReceivedMessageInThreadUnread
*/
export async function markLastReceivedMessageInThreadUnread(
  request: {
    accountId: MessageAccountId,
    threadId: MessageThreadId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/messages/${request.accountId}/threads/${request.threadId}/last-received-message/unread`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.markThreadRead
*/
export async function markThreadRead(
  request: {
    accountId: MessageAccountId,
    threadId: MessageThreadId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/messages/${request.accountId}/threads/${request.threadId}/read`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.moveThreadToFolder
*/
export async function moveThreadToFolder(
  request: {
    accountId: MessageAccountId,
    threadId: MessageThreadId,
    folderId: MessageThreadFolderId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/messages/${request.accountId}/threads/${request.threadId}/move-to-folder/${request.folderId}`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.replyToThread
*/
export async function replyToThread(
  request: {
    accountId: MessageAccountId,
    messageId: MessageId,
    body: ReplyToMessageBody
  }
): Promise<ThreadReply> {
  const { data: json } = await client.request<JsonOf<ThreadReply>>({
    url: uri`/employee/messages/${request.accountId}/${request.messageId}/reply`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ReplyToMessageBody>
  })
  return deserializeJsonThreadReply(json)
}


/**
* Generated from fi.espoo.evaka.messaging.MessageController.updateDraftMessage
*/
export async function updateDraftMessage(
  request: {
    accountId: MessageAccountId,
    draftId: MessageDraftId,
    body: UpdatableDraftContent
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/messages/${request.accountId}/drafts/${request.draftId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<UpdatableDraftContent>
  })
  return json
}
