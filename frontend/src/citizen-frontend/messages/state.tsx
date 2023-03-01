// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import uniqBy from 'lodash/uniqBy'
import React, {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState
} from 'react'

import { Failure, Loading, Result, Success } from 'lib-common/api'
import {
  MessageThread,
  ThreadReply
} from 'lib-common/generated/api-types/messaging'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { useInfiniteQuery, useMutation, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { useUser } from '../auth/state'
import { useTranslation } from '../localization'

import { ReplyToThreadParams } from './api'
import {
  markThreadReadMutation,
  messageAccountQuery,
  receivedMessagesQuery,
  replyToThreadMutation
} from './queries'

export interface MessagePageState {
  accountId: Result<UUID>
  threads: MessageThread[]
  threadLoadingResult: Result<unknown>
  loadMoreThreads: () => void
  selectedThread: MessageThread | undefined
  setSelectedThread: (threadId: UUID | undefined) => void
  sendReply: (params: ReplyToThreadParams) => void
  replyState: Result<void> | undefined
  setReplyContent: (threadId: UUID, content: string) => void
  getReplyContent: (threadId: UUID) => string
}

const defaultState: MessagePageState = {
  accountId: Loading.of(),
  threads: [],
  threadLoadingResult: Loading.of(),
  loadMoreThreads: () => undefined,
  selectedThread: undefined,
  setSelectedThread: () => undefined,
  sendReply: () => undefined,
  replyState: undefined,
  getReplyContent: () => '',
  setReplyContent: () => undefined
}

export const MessageContext = createContext<MessagePageState>(defaultState)

const markMatchingThreadRead = (
  threads: MessageThread[],
  id: UUID
): MessageThread[] =>
  threads.map((t) =>
    t.id === id
      ? {
          ...t,
          messages: t.messages.map((m) => ({
            ...m,
            readAt: m.readAt ?? HelsinkiDateTime.now()
          }))
        }
      : t
  )

export const MessageContextProvider = React.memo(
  function MessageContextProvider({ children }: { children: React.ReactNode }) {
    const t = useTranslation()

    const isLoggedIn = useUser() !== undefined
    const accountId = useQueryResult(messageAccountQuery, {
      enabled: isLoggedIn
    })

    const { data, isLoading, isError, fetchNextPage, transformPages } =
      useInfiniteQuery(receivedMessagesQuery(t.messages.staffAnnotation, 10), {
        enabled: accountId.isSuccess
      })
    const threads = useMemo(() => {
      if (!data) return []
      return uniqBy(
        data.pages.flatMap((p) => p.data),
        'id'
      )
    }, [data])

    const threadLoadingResult = useMemo(
      () =>
        isLoading
          ? Loading.of()
          : isError
          ? Failure.of({ message: '' })
          : Success.of(),
      [isError, isLoading]
    )

    const [selectedThreadId, setSelectedThreadId] = useState<UUID>()

    const [replyContents, setReplyContents] = useState<Record<UUID, string>>({})
    const getReplyContent = useCallback(
      (threadId: UUID) => replyContents[threadId] ?? '',
      [replyContents]
    )
    const setReplyContent = useCallback((threadId: UUID, content: string) => {
      setReplyContents((state) => ({ ...state, [threadId]: content }))
    }, [])

    const [replyState, setReplyState] = useState<Result<void>>()
    const { mutate: sendReply } = useMutation(replyToThreadMutation, {
      onMutate: () => setReplyState(Loading.of()),
      onSuccess: ({ message, threadId }: ThreadReply) => {
        setReplyState(Success.of(undefined))
        transformPages((page) => ({
          ...page,
          data: page.data.map((thread) =>
            thread.id === threadId
              ? { ...thread, messages: [...thread.messages, message] }
              : thread
          )
        }))
        setReplyContents((state) => ({ ...state, [threadId]: '' }))
      }
    })

    const selectedThread = useMemo(
      () =>
        selectedThreadId !== undefined
          ? threads.find((t) => t.id === selectedThreadId)
          : undefined,
      [selectedThreadId, threads]
    )

    const { mutate: markThreadRead } = useMutation(markThreadReadMutation)
    useEffect(() => {
      if (!accountId.isSuccess) return
      if (!selectedThreadId || !selectedThread) return

      const hasUnreadMessages = selectedThread?.messages.some(
        (m) => !m.readAt && m.sender.id !== accountId.value
      )

      if (hasUnreadMessages) {
        markThreadRead(selectedThread.id)
        transformPages((page) => ({
          ...page,
          data: markMatchingThreadRead(page.data, selectedThreadId)
        }))
      }
    }, [
      accountId,
      markThreadRead,
      selectedThread,
      selectedThreadId,
      transformPages
    ])

    const value = useMemo(
      () => ({
        accountId,
        threads,
        threadLoadingResult,
        getReplyContent,
        setReplyContent,
        loadMoreThreads: fetchNextPage,
        selectedThread,
        setSelectedThread: setSelectedThreadId,
        replyState,
        sendReply
      }),
      [
        accountId,
        fetchNextPage,
        getReplyContent,
        replyState,
        selectedThread,
        sendReply,
        setReplyContent,
        threadLoadingResult,
        threads
      ]
    )

    return (
      <MessageContext.Provider value={value}>
        {children}
      </MessageContext.Provider>
    )
  }
)
