// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Loading, Paged, Result, Success } from 'lib-common/api'
import {
  MessageThread,
  ReplyResponse
} from 'lib-common/api-types/messaging/message'
import { UUID } from 'lib-common/types'
import { useRestApi } from 'lib-common/utils/useRestApi'
import React, {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState
} from 'react'
import {
  getMessageAccount,
  getReceivedMessages,
  getUnreadMessagesCount,
  markThreadRead,
  replyToThread,
  ReplyToThreadParams
} from './api'

const initialThreadState: ThreadsState = {
  threads: [],
  selectedThread: undefined,
  loadingResult: Loading.of(),
  currentPage: 0,
  pages: Infinity
}

interface ThreadsState {
  threads: MessageThread[]
  selectedThread: UUID | undefined
  loadingResult: Result<void>
  currentPage: number
  pages: number
}

export interface MessagePageState {
  accountId: Result<UUID>
  loadAccount: () => void
  threads: MessageThread[]
  refreshThreads: () => void
  threadLoadingResult: Result<void>
  loadMoreThreads: () => void
  selectedThread: MessageThread | undefined
  selectThread: (thread: MessageThread | undefined) => void
  sendReply: (params: ReplyToThreadParams) => void
  replyState: Result<void> | undefined
  setReplyContent: (threadId: UUID, content: string) => void
  getReplyContent: (threadId: UUID) => string
  unreadMessagesCount: number | undefined
  refreshUnreadMessagesCount: () => void
}

const defaultState: MessagePageState = {
  accountId: Loading.of(),
  loadAccount: () => undefined,
  threads: [],
  refreshThreads: () => undefined,
  threadLoadingResult: Loading.of(),
  loadMoreThreads: () => undefined,
  selectedThread: undefined,
  selectThread: () => undefined,
  sendReply: () => undefined,
  replyState: undefined,
  getReplyContent: () => '',
  setReplyContent: () => undefined,
  unreadMessagesCount: undefined,
  refreshUnreadMessagesCount: () => undefined
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
            readAt: m.readAt || new Date()
          }))
        }
      : t
  )

export const MessageContextProvider = React.memo(
  function MessageContextProvider({ children }: { children: React.ReactNode }) {
    const [accountId, setAccountId] = useState<Result<UUID>>(Loading.of())
    const loadAccount = useRestApi(getMessageAccount, setAccountId)

    const [threads, setThreads] = useState<ThreadsState>(initialThreadState)

    const setMessagesResult = useCallback(
      (result: Result<Paged<MessageThread>>) =>
        setThreads((state) =>
          result.mapAll({
            loading: () => state,
            failure: () => ({
              ...state,
              loadingResult: result.map(() => undefined)
            }),
            success: ({ data, pages }) => ({
              ...state,
              threads: [...state.threads, ...data],
              loadingResult: Success.of(undefined),
              pages
            })
          })
        ),
      []
    )

    const loadMessages = useRestApi(getReceivedMessages, setMessagesResult)
    const refreshThreads = useCallback(() => {
      setThreads({ ...initialThreadState, currentPage: 0 })
      setThreads((threads) => ({ ...threads, currentPage: 1 }))
    }, [])

    useEffect(() => {
      if (threads.currentPage > 0) {
        setThreads((state) => ({ ...state, loadingResult: Loading.of() }))
        loadMessages(threads.currentPage)
      }
    }, [loadMessages, threads.currentPage])

    const loadMoreThreads = useCallback(() => {
      if (threads.currentPage < threads.pages) {
        setThreads((state) => ({
          ...state,
          currentPage: state.currentPage + 1
        }))
      }
    }, [threads.currentPage, threads.pages])

    useEffect(() => {
      if (accountId.isSuccess) {
        setThreads((state) => ({ ...state, currentPage: 1 }))
      }
    }, [accountId])

    const [replyState, setReplyState] = useState<Result<void>>()
    const setReplyResponse = useCallback((res: Result<ReplyResponse>) => {
      setReplyState(res.map(() => undefined))
      if (res.isSuccess) {
        const {
          value: { message, threadId }
        } = res
        setThreads(function appendMessageAndMoveThreadToTopOfList(state) {
          const thread = state.threads.find((t) => t.id === threadId)
          if (!thread) return state
          const otherThreads = state.threads.filter((t) => t.id !== threadId)
          return {
            ...state,
            threads: [
              {
                ...thread,
                messages: [...thread.messages, message]
              },
              ...otherThreads
            ]
          }
        })
        setReplyContents((state) => ({ ...state, [threadId]: '' }))
      }
    }, [])
    const reply = useRestApi(replyToThread, setReplyResponse)
    const sendReply = useCallback(reply, [reply])

    const [replyContents, setReplyContents] = useState<Record<UUID, string>>({})

    const getReplyContent = useCallback(
      (threadId: UUID) => replyContents[threadId] ?? '',
      [replyContents]
    )
    const setReplyContent = useCallback((threadId: UUID, content: string) => {
      setReplyContents((state) => ({ ...state, [threadId]: content }))
    }, [])

    const [unreadMessagesCount, setUnreadMessagesCount] = useState<number>()
    const setUnreadResult = useCallback((res: Result<number>) => {
      if (res.isSuccess) {
        setUnreadMessagesCount(res.value)
      }
    }, [])
    const refreshUnreadMessagesCount = useRestApi(
      getUnreadMessagesCount,
      setUnreadResult
    )

    const selectThread = useCallback(
      (thread: MessageThread | undefined) => {
        if (!thread) {
          return setThreads((state) => ({
            ...state,
            selectedThread: undefined
          }))
        }

        const hasUnreadMessages =
          !!accountId?.isSuccess &&
          thread.messages.some(
            (m) => !m.readAt && m.senderId !== accountId.value
          )

        setThreads((state) => {
          return {
            ...state,
            threads: markMatchingThreadRead(state.threads, thread.id),
            selectedThread: thread.id
          }
        })

        if (hasUnreadMessages) {
          void markThreadRead(thread.id).then(() => {
            refreshUnreadMessagesCount()
          })
        }
      },
      [accountId, refreshUnreadMessagesCount]
    )

    const selectedThread = useMemo(
      () =>
        threads.selectedThread
          ? threads.threads.find((t) => t.id === threads.selectedThread)
          : undefined,
      [threads.selectedThread, threads.threads]
    )

    const value = useMemo(
      () => ({
        accountId,
        loadAccount,
        threads: threads.threads,
        refreshThreads,
        threadLoadingResult: threads.loadingResult,
        getReplyContent,
        setReplyContent,
        loadMoreThreads,
        selectedThread,
        selectThread,
        replyState,
        sendReply,
        unreadMessagesCount,
        refreshUnreadMessagesCount
      }),
      [
        accountId,
        loadAccount,
        threads.threads,
        refreshThreads,
        threads.loadingResult,
        getReplyContent,
        setReplyContent,
        loadMoreThreads,
        selectedThread,
        selectThread,
        replyState,
        sendReply,
        unreadMessagesCount,
        refreshUnreadMessagesCount
      ]
    )

    return (
      <MessageContext.Provider value={value}>
        {children}
      </MessageContext.Provider>
    )
  }
)
