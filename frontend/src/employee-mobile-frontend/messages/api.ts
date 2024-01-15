// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  deserializeMessageThread,
  deserializeReplyResponse
} from 'lib-common/api-types/messaging'
import {
  AuthorizedMessageAccount,
  DraftContent,
  MessageReceiversResponse,
  MessageThread,
  PagedMessageThreads,
  PagedSentMessages,
  PostMessageBody,
  ReplyToMessageBody,
  ThreadReply,
  UnreadCountByAccountAndGroup
} from 'lib-common/generated/api-types/messaging'
import { JsonOf } from 'lib-common/json'
import {
  deserializeDraftContent,
  deserializeSentMessage
} from 'lib-common/messaging'
import { UUID } from 'lib-common/types'
import { SaveDraftParams } from 'lib-components/messages/types'

import { API_URL, client } from '../client'

export async function getMessagingAccounts(
  unitId: UUID
): Promise<AuthorizedMessageAccount[]> {
  return client
    .get<
      JsonOf<AuthorizedMessageAccount[]>
    >(`/messages/mobile/my-accounts/${unitId}`)
    .then((res) => res.data)
}

export async function getUnreadCountsByUnit(
  unitId: UUID
): Promise<UnreadCountByAccountAndGroup[]> {
  return client
    .get<JsonOf<UnreadCountByAccountAndGroup[]>>(`/messages/unread/${unitId}`)
    .then((res) => res.data)
}

export async function getReceivedMessages(
  accountId: UUID,
  page: number,
  pageSize: number
): Promise<PagedMessageThreads> {
  return client
    .get<JsonOf<PagedMessageThreads>>(`/messages/${accountId}/received`, {
      params: { page, pageSize }
    })
    .then(({ data }) => ({
      ...data,
      data: data.data.map((d) => deserializeMessageThread(d))
    }))
}

export async function getSentMessages(
  accountId: UUID,
  page: number,
  pageSize: number
): Promise<PagedSentMessages> {
  return client
    .get<JsonOf<PagedSentMessages>>(`/messages/${accountId}/sent`, {
      params: { page, pageSize }
    })
    .then(({ data }) => ({
      ...data,
      data: data.data.map(deserializeSentMessage)
    }))
}

export async function getMessageDrafts(
  accountId: UUID
): Promise<DraftContent[]> {
  return client
    .get<JsonOf<DraftContent[]>>(`/messages/${accountId}/drafts`)
    .then(({ data }) => data.map(deserializeDraftContent))
}

export async function getThread(
  accountId: UUID,
  threadId: UUID
): Promise<MessageThread> {
  return client
    .get<JsonOf<MessageThread>>(`/messages/${accountId}/thread/${threadId}`)
    .then(({ data }) => deserializeMessageThread(data))
}

export type ReplyToThreadParams = ReplyToMessageBody & {
  threadId: UUID
  messageId: UUID
  accountId: UUID
}

export async function replyToThread({
  messageId,
  content,
  accountId,
  recipientAccountIds
}: ReplyToThreadParams): Promise<ThreadReply> {
  return client
    .post<JsonOf<ThreadReply>>(`/messages/${accountId}/${messageId}/reply`, {
      content,
      recipientAccountIds
    })
    .then(({ data }) => deserializeReplyResponse(data))
}

export async function markThreadRead({
  accountId,
  id
}: {
  accountId: UUID
  id: UUID
}): Promise<void> {
  return client
    .put(`/messages/${accountId}/threads/${id}/read`)
    .then(() => undefined)
}

export async function sendMessage({
  accountId,
  body
}: {
  accountId: UUID
  body: PostMessageBody
}): Promise<void> {
  return client.post(`/messages/${accountId}`, body).then(() => undefined)
}

export async function initDraft(accountId: UUID): Promise<UUID> {
  return client
    .post<UUID>(`/messages/${accountId}/drafts`)
    .then((res) => res.data)
}

export async function saveDraft({
  accountId,
  draftId,
  content
}: SaveDraftParams): Promise<void> {
  return client
    .put(`/messages/${accountId}/drafts/${draftId}`, content)
    .then(() => undefined)
}

export async function deleteDraft({
  accountId,
  draftId
}: {
  accountId: UUID
  draftId: UUID
}): Promise<void> {
  return client
    .delete(`/messages/${accountId}/drafts/${draftId}`)
    .then(() => undefined)
}

export async function getReceivers(): Promise<MessageReceiversResponse[]> {
  return client
    .get<JsonOf<MessageReceiversResponse[]>>('/messages/receivers')
    .then((res) => res.data)
}

async function doSaveAttachment(
  config: { path: string; params?: unknown },
  file: File,
  onUploadProgress: (percentage: number) => void
): Promise<Result<UUID>> {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const { data } = await client.post<UUID>(config.path, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      params: config.params,
      onUploadProgress: ({ loaded, total }) =>
        onUploadProgress(
          total !== undefined && total !== 0
            ? Math.round((loaded * 100) / total)
            : 0
        )
    })
    return Success.of(data)
  } catch (e) {
    return Failure.fromError(e)
  }
}

export const saveMessageAttachment = (
  draftId: UUID,
  file: File,
  onUploadProgress: (percentage: number) => void
): Promise<Result<UUID>> =>
  doSaveAttachment(
    { path: `/attachments/messages/${draftId}` },
    file,
    onUploadProgress
  )

export const deleteAttachment = (id: UUID): Promise<Result<void>> =>
  client
    .delete(`/attachments/${id}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))

export function getAttachmentUrl(attachmentId: UUID): string {
  return `${API_URL}/attachments/${attachmentId}/download`
}
