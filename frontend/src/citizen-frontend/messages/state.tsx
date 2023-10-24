// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState
} from 'react'

import { Failure, Loading, Result } from 'lib-common/api'
import { CitizenMessageThread } from 'lib-common/generated/api-types/messaging'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import {
  useMutation,
  useMutationResult,
  usePagedInfiniteQueryResult,
  useQueryResult
} from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { useUser } from '../auth/state'

import { ReplyToThreadParams } from './api'
import {
  markThreadReadMutation,
  messageAccountQuery,
  receivedMessagesQuery,
  replyToThreadMutation
} from './queries'

export interface MessagePageState {
  accountId: Result<UUID>
  threads: Result<CitizenMessageThread[]>
  hasMoreThreads: boolean
  loadMoreThreads: () => void
  selectedThread: CitizenMessageThread | undefined
  setSelectedThread: (threadId: UUID | undefined) => void
  sendReply: (params: ReplyToThreadParams) => Promise<Result<unknown>>
  setReplyContent: (threadId: UUID, content: string) => void
  getReplyContent: (threadId: UUID) => string
}

const defaultState: MessagePageState = {
  accountId: Loading.of(),
  threads: Loading.of(),
  loadMoreThreads: () => undefined,
  hasMoreThreads: false,
  selectedThread: undefined,
  setSelectedThread: () => undefined,
  sendReply: () => Promise.resolve(Failure.of({ message: 'Not initialized' })),
  getReplyContent: () => '',
  setReplyContent: () => undefined
}

export const MessageContext = createContext<MessagePageState>(defaultState)

export const isRedactedThread = (
  thread: CitizenMessageThread
): thread is CitizenMessageThread.Redacted => thread.type === 'Redacted'
export const isRegularThread = (
  thread: CitizenMessageThread
): thread is CitizenMessageThread.Regular => thread.type === 'Regular'

const markMessagesReadByThreadId = (
  thread: CitizenMessageThread,
  threadId: UUID
): CitizenMessageThread =>
  isRegularThread(thread) && thread.id === threadId
    ? {
        ...thread,
        messages: thread.messages.map((m) => ({
          ...m,
          readAt: m.readAt ?? HelsinkiDateTime.now()
        }))
      }
    : thread

export const MessageContextProvider = React.memo(
  function MessageContextProvider({ children }: { children: React.ReactNode }) {
    const isLoggedIn = useUser() !== undefined
    const accountId = useQueryResult(messageAccountQuery(), {
      enabled: isLoggedIn,
      staleTime: 24 * 60 * 60 * 1000
    })

    const {
      data: threads,
      fetchNextPage,
      hasNextPage,
      transform
    } = usePagedInfiniteQueryResult(receivedMessagesQuery(10), {
      enabled: accountId.isSuccess
    })

    const [selectedThreadId, setSelectedThreadId] = useState<UUID>()

    const [replyContents, setReplyContents] = useState<Record<UUID, string>>({})
    const getReplyContent = useCallback(
      (threadId: UUID) => replyContents[threadId] ?? '',
      [replyContents]
    )
    const setReplyContent = useCallback((threadId: UUID, content: string) => {
      setReplyContents((state) => ({ ...state, [threadId]: content }))
    }, [])

    const { mutateAsync: sendReply } = useMutationResult(replyToThreadMutation)
    const sendReplyAndClear = useCallback(
      async (arg: ReplyToThreadParams) => {
        const result = await sendReply(arg)
        if (result.isSuccess) {
          setReplyContents((state) => ({
            ...state,
            [result.value.threadId]: ''
          }))
        }
        return result
      },
      [sendReply]
    )

    const selectedThread = useMemo(
      () =>
        selectedThreadId !== undefined
          ? threads
              .map((threads) => threads.find((t) => t.id === selectedThreadId))
              .getOrElse(undefined)
          : undefined,
      [selectedThreadId, threads]
    )

    const { mutate: markThreadRead } = useMutation(markThreadReadMutation)
    useEffect(() => {
      if (!accountId.isSuccess) return
      if (!selectedThreadId || !selectedThread) return

      const hasUnreadMessages = isRedactedThread(selectedThread)
        ? selectedThread.hasUnreadMessages
        : selectedThread.messages.some(
            (m) => !m.readAt && m.sender.id !== accountId.value
          )

      if (hasUnreadMessages && isRegularThread(selectedThread)) {
        markThreadRead(selectedThread.id)
        transform((t) => markMessagesReadByThreadId(t, selectedThreadId))
      }
    }, [accountId, markThreadRead, selectedThread, selectedThreadId, transform])

    const value = useMemo(
      () => ({
        accountId,
        threads,
        getReplyContent,
        setReplyContent,
        loadMoreThreads: () => {
          if (hasNextPage) {
            void fetchNextPage()
          }
        },
        hasMoreThreads: hasNextPage !== undefined && hasNextPage,
        selectedThread,
        setSelectedThread: setSelectedThreadId,
        sendReply: sendReplyAndClear
      }),
      [
        accountId,
        fetchNextPage,
        getReplyContent,
        hasNextPage,
        selectedThread,
        sendReplyAndClear,
        setReplyContent,
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
