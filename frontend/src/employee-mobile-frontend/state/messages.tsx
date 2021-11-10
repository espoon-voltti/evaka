// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Loading, Paged, Result } from 'lib-common/api'
import {
  Message,
  MessageThread,
  NestedMessageAccount,
  ThreadReply,
  UnreadCountByAccount
} from 'lib-common/generated/api-types/messaging'
import { MessageAccount } from 'lib-common/generated/api-types/messaging'
import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { useRestApi } from 'lib-common/utils/useRestApi'
import {
  getMessagingAccounts,
  getReceivedMessages,
  getUnreadCounts,
  markThreadRead,
  replyToThread,
  ReplyToThreadParams
} from '../api/messages'
import { useDebouncedCallback } from 'lib-common/utils/useDebouncedCallback'
import { UserContext } from './user'
import { UUID } from 'lib-common/types'
import { UnitContext } from './unit'
import { SelectOption } from 'lib-components/molecules/Select'

const PAGE_SIZE = 20

const addMessageToThread = (message: Message, thread: MessageThread) => ({
  ...thread,
  messages: [...thread.messages, message]
})

const appendMessageAndMoveThreadToTopOfList =
  (threadId: UUID, message: Message) => (state: Result<MessageThread[]>) =>
    state.map((threads) => {
      const thread = threads.find((t) => t.id === threadId)
      if (!thread) return threads
      const otherThreads = threads.filter((t) => t.id !== threadId)
      return [addMessageToThread(message, thread), ...otherThreads]
    })

export interface MessagesState {
  nestedAccounts: Result<NestedMessageAccount[]>
  loadNestedAccounts: (unitId: UUID) => void
  page: number
  setPage: (page: number) => void
  pages: number | undefined
  setPages: (pages: number) => void
  selectedAccount: MessageAccount | undefined
  setSelectedAccount: (account: MessageAccount) => void
  preselectAccount: () => void
  receivedMessages: Result<MessageThread[]>
  refreshMessages: (account?: UUID) => void
  selectedUnit: SelectOption | undefined
  selectedThread: MessageThread | undefined
  selectThread: (thread: MessageThread | undefined) => void
  sendReply: (params: ReplyToThreadParams) => void
  replyState: Result<void> | undefined
  setReplyContent: (threadId: UUID, content: string) => void
  getReplyContent: (threadId: UUID) => string
  unreadCountsByAccount: Result<UnreadCountByAccount[]>
}

const defaultState: MessagesState = {
  nestedAccounts: Loading.of(),
  loadNestedAccounts: () => undefined,
  page: 1,
  setPage: () => undefined,
  pages: undefined,
  setPages: () => undefined,
  selectedAccount: undefined,
  setSelectedAccount: () => undefined,
  preselectAccount: () => undefined,
  receivedMessages: Loading.of(),
  refreshMessages: () => undefined,
  selectedUnit: undefined,
  selectedThread: undefined,
  selectThread: () => undefined,
  sendReply: () => undefined,
  replyState: undefined,
  getReplyContent: () => '',
  setReplyContent: () => undefined,
  unreadCountsByAccount: Loading.of()
}

export const MessageContext = createContext<MessagesState>(defaultState)

function getThreadUnreadCount(thread: MessageThread, accountId: string) {
  return thread.messages.reduce(
    (sum, m) => (!m.readAt && m.sender.id !== accountId ? sum + 1 : sum),
    0
  )
}

function adjustUnreadCounts(
  counts: UnreadCountByAccount[],
  accountId: string,
  threadUnreadCount: number
) {
  return counts.map(({ accountId: acc, unreadCount }) =>
    acc === accountId
      ? {
          accountId: acc,
          unreadCount: unreadCount - threadUnreadCount
        }
      : { accountId: acc, unreadCount }
  )
}

export const MessageContextProvider = React.memo(
  function MessageContextProvider({ children }: { children: JSX.Element }) {
    const [page, setPage] = useState<number>(1)
    const [pages, setPages] = useState<number>()
    const { user, loggedIn } = useContext(UserContext)
    const { unitInfoResponse } = useContext(UnitContext)

    const unitId = unitInfoResponse.map((res) => res.id).getOrElse(undefined)
    const [selectedUnit, setSelectedUnit] = useState<SelectOption | undefined>()

    useEffect(() => {
      const unit = unitInfoResponse.getOrElse(undefined)
      setSelectedUnit(unit && { value: unit.id, label: unit.name })
    }, [unitInfoResponse])

    const [nestedAccounts, setNestedMessagingAccounts] = useState<
      Result<NestedMessageAccount[]>
    >(Loading.of())

    const getNestedAccounts = useRestApi(
      getMessagingAccounts,
      setNestedMessagingAccounts
    )

    const loadNestedAccounts = useDebouncedCallback(getNestedAccounts, 100)

    const [selectedAccount, setSelectedAccount] = useState<MessageAccount>()

    const preselectAccount = useCallback(() => {
      if (!nestedAccounts.isSuccess) {
        return
      }
      const { value: data } = nestedAccounts
      const unitSelectionChange =
        selectedAccount &&
        !data.find(
          (nestedAccount) => nestedAccount.account.id === selectedAccount.id
        )
      if ((!selectedAccount || unitSelectionChange) && data.length > 0) {
        setSelectedAccount(
          data.find((a) => a.account.type === 'PERSONAL')?.account ||
            data[0].account
        )
      }
    }, [nestedAccounts, selectedAccount, setSelectedAccount])

    useEffect(preselectAccount, [
      nestedAccounts,
      preselectAccount,
      setSelectedAccount,
      selectedAccount
    ])

    const [unreadCountsByAccount, setUnreadCountsByAccount] = useState<
      Result<UnreadCountByAccount[]>
    >(Loading.of())

    const loadUnreadCounts = useRestApi(
      getUnreadCounts,
      setUnreadCountsByAccount
    )

    const userHasPin = !!(
      user.isSuccess && user.map((item) => item?.employeeId).getOrElse(false)
    )

    useEffect(() => {
      loggedIn && userHasPin && user.map((item) => item?.employeeId) && unitId
        ? loadUnreadCounts()
        : null
    }, [loadUnreadCounts, userHasPin, loggedIn, unitId])

    const [receivedMessages, setReceivedMessages] = useState<
      Result<MessageThread[]>
    >(Loading.of())

    const setReceivedMessagesResult = useCallback(
      (result: Result<Paged<MessageThread>>) => {
        setReceivedMessages(result.map((r) => r.data))
        if (result.isSuccess) {
          setPages(result.value.pages)
        }
      },
      []
    )

    const loadReceivedMessages = useRestApi(
      getReceivedMessages,
      setReceivedMessagesResult
    )

    // load messages if account, view or page changes
    const loadMessages = useCallback(() => {
      if (!selectedAccount) {
        return
      }
      loadReceivedMessages(selectedAccount.id, page, PAGE_SIZE)
    }, [loadReceivedMessages, page, selectedAccount])

    const refreshMessages = useCallback(
      (accountId?: UUID) => {
        if (!accountId || selectedAccount?.id === accountId) {
          loadMessages()
        }
      },
      [loadMessages, selectedAccount]
    )

    const [selectedThread, setSelectedThread] = useState<MessageThread>()

    const selectThread = useCallback(
      (thread: MessageThread | undefined) => {
        setSelectedThread(thread)
        if (!thread) return
        if (!selectedAccount) throw new Error('Should never happen')

        const { id: accountId } = selectedAccount

        const threadUnreadCount = getThreadUnreadCount(thread, accountId)
        if (threadUnreadCount > 0) {
          setUnreadCountsByAccount((request) =>
            request.map((result) =>
              adjustUnreadCounts(result, accountId, threadUnreadCount)
            )
          )
        }

        void markThreadRead(accountId, thread.id).then(() =>
          refreshMessages(accountId)
        )
      },
      [refreshMessages, selectedAccount, setUnreadCountsByAccount]
    )

    const [replyContents, setReplyContents] = useState<Record<UUID, string>>({})

    const getReplyContent = useCallback(
      (threadId: UUID) => replyContents[threadId] ?? '',
      [replyContents]
    )
    const setReplyContent = useCallback((threadId: UUID, content: string) => {
      setReplyContents((state) => ({ ...state, [threadId]: content }))
    }, [])

    const [replyState, setReplyState] = useState<Result<void>>()
    const setReplyResponse = useCallback((res: Result<ThreadReply>) => {
      setReplyState(res.map(() => undefined))
      if (res.isSuccess) {
        const {
          value: { message, threadId }
        } = res
        setReceivedMessages(
          appendMessageAndMoveThreadToTopOfList(threadId, message)
        )
        setSelectedThread((thread) =>
          thread?.id === threadId ? addMessageToThread(message, thread) : thread
        )
        setReplyContents((state) => ({ ...state, [threadId]: '' }))
      }
    }, [])
    const reply = useRestApi(replyToThread, setReplyResponse)
    const sendReply = useCallback(reply, [reply])

    const value = useMemo(
      () => ({
        nestedAccounts,
        loadNestedAccounts,
        selectedAccount,
        setSelectedAccount,
        preselectAccount,
        page,
        setPage,
        pages,
        setPages,
        receivedMessages,
        refreshMessages,
        selectedUnit,
        selectThread,
        selectedThread,
        unreadCountsByAccount,
        getReplyContent,
        sendReply,
        setReplyContent,
        replyState
      }),
      [
        nestedAccounts,
        loadNestedAccounts,
        selectedAccount,
        page,
        pages,
        preselectAccount,
        receivedMessages,
        refreshMessages,
        selectedUnit,
        selectedThread,
        selectThread,
        unreadCountsByAccount,
        getReplyContent,
        sendReply,
        setReplyContent,
        replyState
      ]
    )

    return (
      <MessageContext.Provider value={value}>
        {children}
      </MessageContext.Provider>
    )
  }
)
