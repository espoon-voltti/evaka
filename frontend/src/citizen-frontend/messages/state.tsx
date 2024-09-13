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

import { Loading, Result } from 'lib-common/api'
import {
  CitizenMessageThread,
  MyAccountResponse
} from 'lib-common/generated/api-types/messaging'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import {
  useMutation,
  usePagedInfiniteQueryResult,
  useQueryResult
} from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { useUser } from '../auth/state'

import {
  markThreadReadMutation,
  messageAccountQuery,
  receivedMessagesQuery
} from './queries'

export interface MessagePageState {
  messageAccount: Result<MyAccountResponse>
  threads: Result<CitizenMessageThread[]>
  hasMoreThreads: boolean
  loadMoreThreads: () => void
  selectedThread: CitizenMessageThread | undefined
  setSelectedThread: (threadId: UUID | undefined) => void
  setReplyContent: (threadId: UUID, content: string) => void
  getReplyContent: (threadId: UUID) => string
}

const defaultState: MessagePageState = {
  messageAccount: Loading.of(),
  threads: Loading.of(),
  loadMoreThreads: () => undefined,
  hasMoreThreads: false,
  selectedThread: undefined,
  setSelectedThread: () => undefined,
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
    const messageAccount = useQueryResult(messageAccountQuery(), {
      enabled: isLoggedIn,
      staleTime: 24 * 60 * 60 * 1000
    })

    const {
      data: threads,
      fetchNextPage,
      hasNextPage,
      transform
    } = usePagedInfiniteQueryResult(receivedMessagesQuery(), {
      enabled: messageAccount.isSuccess
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
      if (!messageAccount.isSuccess) return
      if (!selectedThreadId || !selectedThread) return

      if (isRegularThread(selectedThread)) {
        const hasUnreadMessages = selectedThread.messages.some(
          (m) => !m.readAt && m.sender.id !== messageAccount.value.accountId
        )
        if (hasUnreadMessages) {
          markThreadRead({ threadId: selectedThread.id })
          transform((t) => markMessagesReadByThreadId(t, selectedThreadId))
        }
      }
    }, [
      messageAccount,
      markThreadRead,
      selectedThread,
      selectedThreadId,
      transform
    ])

    const value = useMemo(
      () => ({
        messageAccount,
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
        setSelectedThread: setSelectedThreadId
      }),
      [
        messageAccount,
        fetchNextPage,
        getReplyContent,
        hasNextPage,
        selectedThread,
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
