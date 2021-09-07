{
  /*
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import { SelectOption } from 'employee-frontend/components/common/Select'
import { UserContext } from 'employee-frontend/state/user'
import { Loading, Paged, Result } from 'lib-common/api'
import {
  Message,
  MessageThread,
  ReplyResponse
} from 'lib-common/api-types/messaging/message'
import { useDebouncedCallback } from 'lib-common/utils/useDebouncedCallback'
import { useRestApi } from 'lib-common/utils/useRestApi'
import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { UUID } from '../../types'
import {
  getMessageDrafts,
  getMessagingAccounts as getNestedMessagingAccounts,
  getReceivedMessages,
  getSentMessages,
  getUnreadCounts,
  markThreadRead,
  replyToThread,
  ReplyToThreadParams
} from './api'
import {
  DraftContent,
  NestedMessageAccount,
  SentMessage,
  UnreadCountByAccount
} from './types'
import { AccountView } from './types-view'

const PAGE_SIZE = 20
type RepliesByThread = Record<UUID, string>

export interface MessagesState {
  nestedAccounts: Result<NestedMessageAccount[]>
  loadNestedAccounts: () => void
  selectedDraft: DraftContent | undefined
  setSelectedDraft: (draft: DraftContent | undefined) => void
  selectedAccount: AccountView | undefined
  setSelectedAccount: (view: AccountView) => void
  selectedUnit: SelectOption | undefined
  setSelectedUnit: (unit: SelectOption) => void
  page: number
  setPage: (page: number) => void
  pages: number | undefined
  setPages: (pages: number) => void
  receivedMessages: Result<MessageThread[]>
  sentMessages: Result<SentMessage[]>
  messageDrafts: Result<DraftContent[]>
  selectedThread: MessageThread | undefined
  selectThread: (thread: MessageThread | undefined) => void
  sendReply: (params: ReplyToThreadParams) => void
  replyState: Result<void> | undefined
  setReplyContent: (threadId: UUID, content: string) => void
  getReplyContent: (threadId: UUID) => string
  refreshMessages: (account?: UUID) => void
  unreadCountsByAccount: Result<UnreadCountByAccount[]>
}

const defaultState: MessagesState = {
  nestedAccounts: Loading.of(),
  loadNestedAccounts: () => undefined,
  selectedDraft: undefined,
  setSelectedDraft: () => undefined,
  selectedAccount: undefined,
  setSelectedAccount: () => undefined,
  selectedUnit: undefined,
  setSelectedUnit: () => undefined,
  page: 1,
  setPage: () => undefined,
  pages: undefined,
  setPages: () => undefined,
  receivedMessages: Loading.of(),
  sentMessages: Loading.of(),
  messageDrafts: Loading.of(),
  selectedThread: undefined,
  selectThread: () => undefined,
  sendReply: () => undefined,
  replyState: undefined,
  getReplyContent: () => '',
  setReplyContent: () => undefined,
  refreshMessages: () => undefined,
  unreadCountsByAccount: Loading.of()
}

export const MessageContext = createContext<MessagesState>(defaultState)

const appendMessageAndMoveThreadToTopOfList =
  (threadId: UUID, message: Message) => (state: Result<MessageThread[]>) =>
    state.map((threads) => {
      const thread = threads.find((t) => t.id === threadId)
      if (!thread) return threads
      const otherThreads = threads.filter((t) => t.id !== threadId)
      return [
        {
          ...thread,
          messages: [...thread.messages, message]
        },
        ...otherThreads
      ]
    })

export const MessageContextProvider = React.memo(
  function MessageContextProvider({ children }: { children: JSX.Element }) {
    const [selectedUnit, setSelectedUnit] = useState<SelectOption | undefined>()
    const { loggedIn } = useContext(UserContext)

    const [nestedAccounts, setNestedMessagingAccounts] = useState<
      Result<NestedMessageAccount[]>
    >(Loading.of())

    const [unreadCountsByAccount, setUnreadCountsByAccount] = useState<
      Result<UnreadCountByAccount[]>
    >(Loading.of())

    const getNestedAccounts = useRestApi(
      getNestedMessagingAccounts,
      setNestedMessagingAccounts
    )
    const loadNestedAccounts = useDebouncedCallback(getNestedAccounts, 100)

    const loadUnreadCounts = useRestApi(
      getUnreadCounts,
      setUnreadCountsByAccount
    )

    useEffect(() => {
      loggedIn ? loadNestedAccounts() : null
    }, [loadNestedAccounts, loggedIn])

    useEffect(() => {
      loggedIn ? loadUnreadCounts() : null
    }, [loadUnreadCounts, loggedIn])

    const [selectedAccount, setSelectedAccount] = useState<AccountView>()
    const [selectedDraft, setSelectedDraft] = useState(
      defaultState.selectedDraft
    )

    const [selectedThread, setSelectedThread] = useState<MessageThread>()

    const [page, setPage] = useState<number>(1)
    const [pages, setPages] = useState<number>()
    const [receivedMessages, setReceivedMessages] = useState<
      Result<MessageThread[]>
    >(Loading.of())
    const [messageDrafts, setMessageDrafts] = useState<Result<DraftContent[]>>(
      Loading.of()
    )
    const [sentMessages, setSentMessages] = useState<Result<SentMessage[]>>(
      Loading.of()
    )

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

    const loadMessageDrafts = useRestApi(getMessageDrafts, setMessageDrafts)

    const setSentMessagesResult = useCallback(
      (result: Result<Paged<SentMessage>>) => {
        setSentMessages(result.map((r) => r.data))
        if (result.isSuccess) {
          setPages(result.value.pages)
        }
      },
      []
    )
    const loadSentMessages = useRestApi(getSentMessages, setSentMessagesResult)

    // load messages if account, view or page changes
    const loadMessages = useCallback(() => {
      if (!selectedAccount) {
        return
      }
      switch (selectedAccount.view) {
        case 'RECEIVED':
          loadReceivedMessages(selectedAccount.account.id, page, PAGE_SIZE)
          break
        case 'SENT':
          loadSentMessages(selectedAccount.account.id, page, PAGE_SIZE)
          break
        case 'DRAFTS':
          loadMessageDrafts(selectedAccount.account.id)
      }
    }, [
      loadMessageDrafts,
      loadReceivedMessages,
      loadSentMessages,
      page,
      selectedAccount
    ])

    const [replyState, setReplyState] = useState<Result<void>>()
    const setReplyResponse = useCallback((res: Result<ReplyResponse>) => {
      setReplyState(res.map(() => undefined))
      if (res.isSuccess) {
        const {
          value: { message, threadId }
        } = res
        setReceivedMessages(
          appendMessageAndMoveThreadToTopOfList(threadId, message)
        )
        setSelectedThread((thread) =>
          thread?.id === threadId
            ? { ...thread, messages: [...thread.messages, message] }
            : thread
        )
        setReplyContents((state) => ({ ...state, [threadId]: '' }))
      }
    }, [])
    const reply = useRestApi(replyToThread, setReplyResponse)
    const sendReply = useCallback(reply, [reply])

    const [replyContents, setReplyContents] = useState<RepliesByThread>({})

    const getReplyContent = useCallback(
      (threadId: UUID) => replyContents[threadId] ?? '',
      [replyContents]
    )
    const setReplyContent = useCallback((threadId: UUID, content: string) => {
      setReplyContents((state) => ({ ...state, [threadId]: content }))
    }, [])

    const refreshMessages = useCallback(
      (accountId?: UUID) => {
        if (!accountId || selectedAccount?.account.id === accountId) {
          loadMessages()
        }
      },
      [loadMessages, selectedAccount]
    )
    const selectThread = useCallback(
      (thread: MessageThread | undefined) => {
        setSelectedThread(thread)
        if (!thread) return
        if (!selectedAccount) throw new Error('Should never happen')

        const { id: accountId } = selectedAccount.account
        const threadUnreadCount = thread.messages.reduce(
          (sum, m) => (!m.readAt && m.sender.id !== accountId ? sum + 1 : sum),
          0
        )
        if (threadUnreadCount > 0) {
          setUnreadCountsByAccount((request) =>
            request.map((result) =>
              result.map(({ accountId: acc, unreadCount }) =>
                acc === accountId
                  ? {
                      accountId: acc,
                      unreadCount: unreadCount - threadUnreadCount
                    }
                  : { accountId: acc, unreadCount }
              )
            )
          )
        }

        void markThreadRead(accountId, thread.id).then(() =>
          refreshMessages(accountId)
        )
      },
      [refreshMessages, selectedAccount, setUnreadCountsByAccount]
    )

    const value = useMemo(
      () => ({
        nestedAccounts,
        loadNestedAccounts,
        selectedDraft,
        setSelectedDraft,
        selectedAccount,
        setSelectedAccount,
        selectedUnit,
        setSelectedUnit,
        page,
        setPage,
        pages,
        setPages,
        receivedMessages,
        sentMessages,
        messageDrafts,
        selectedThread,
        selectThread,
        replyState,
        sendReply,
        getReplyContent,
        setReplyContent,
        refreshMessages,
        unreadCountsByAccount
      }),
      [
        nestedAccounts,
        loadNestedAccounts,
        selectedDraft,
        selectedAccount,
        selectedUnit,
        setSelectedUnit,
        page,
        pages,
        receivedMessages,
        sentMessages,
        messageDrafts,
        selectedThread,
        selectThread,
        replyState,
        sendReply,
        getReplyContent,
        setReplyContent,
        refreshMessages,
        unreadCountsByAccount
      ]
    )

    return (
      <MessageContext.Provider value={value}>
        {children}
      </MessageContext.Provider>
    )
  }
)
