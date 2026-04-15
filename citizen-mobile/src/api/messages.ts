// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { api } from './client'

export interface ThreadListItem {
  id: string
  title: string
  lastMessagePreview: string
  lastMessageAt: string
  unreadCount: number
  senderName: string
}

export interface ThreadListResponse {
  data: ThreadListItem[]
  hasMore: boolean
}

export interface ThreadMessage {
  id: string
  senderName: string
  senderAccountId: string
  content: string
  sentAt: string
  readAt: string | null
}

export interface Thread {
  id: string
  title: string
  messages: ThreadMessage[]
}

export interface MyAccount {
  accountId: string
  messageAttachmentsAllowed: boolean
}

export const getThreads = (token: string, page = 1, pageSize = 20) =>
  api<ThreadListResponse>(
    `/citizen-mobile/messages/threads/v1?page=${page}&pageSize=${pageSize}`,
    { token }
  )

export const getThread = (token: string, threadId: string) =>
  api<Thread>(`/citizen-mobile/messages/thread/${threadId}/v1`, { token })

export const replyToThread = (
  token: string,
  threadId: string,
  content: string,
  recipientAccountIds: string[]
) =>
  api<void>(`/citizen-mobile/messages/thread/${threadId}/reply/v1`, {
    method: 'POST',
    token,
    body: { content, recipientAccountIds }
  })

export const markThreadRead = (token: string, threadId: string) =>
  api<void>(`/citizen-mobile/messages/thread/${threadId}/mark-read/v1`, {
    method: 'POST',
    token
  })

export const getUnreadCount = (token: string) =>
  api<number>('/citizen-mobile/messages/unread-count/v1', { token })

export const getMyAccount = (token: string) =>
  api<MyAccount>('/citizen-mobile/messages/my-account/v1', { token })
