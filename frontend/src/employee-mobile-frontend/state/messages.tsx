// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Loading, Paged, Result, Success } from 'lib-common/api'
import {
  Message,
  MessageAccount,
  MessageThread,
  NestedMessageAccount,
  ThreadReply,
  UnreadCountByAccount
} from 'lib-common/generated/api-types/messaging'
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
import { GroupInfo } from 'lib-common/generated/api-types/attendance'

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

export const getAccountsByUserAndUnit = (
  groupAccounts: NestedMessageAccount[],
  personalAccount: MessageAccount | undefined
) => {
  return groupAccounts
    .map(({ account }) => account)
    .concat(personalAccount ? [personalAccount] : [])
}

export interface MessagesState {
  nestedAccounts: Result<NestedMessageAccount[]>
  loadNestedAccounts: (unitId: UUID) => void
  page: number
  setPage: (page: number) => void
  pages: number | undefined
  setPages: (pages: number) => void
  groupAccounts: NestedMessageAccount[]
  selectedSender: MessageAccount | undefined
  selectedAccount: MessageAccount | undefined
  personalAccount: MessageAccount | undefined
  setSelectedAccount: (account: MessageAccount | undefined) => void
  receivedMessages: Result<MessageThread[]>
  loadMessagesForAllAccounts: () => void
  loadMessagesForSelectedAccount: () => void
  loadMessagesWhenGroupChanges: (selectedGroup: GroupInfo | undefined) => void
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
  selectedSender: undefined,
  selectedAccount: undefined,
  personalAccount: undefined,
  groupAccounts: [],
  setSelectedAccount: () => undefined,
  receivedMessages: Loading.of(),
  loadMessagesForAllAccounts: () => undefined,
  loadMessagesForSelectedAccount: () => undefined,
  loadMessagesWhenGroupChanges: () => undefined,
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
    const [selectedSender, setSelectedSender] = useState<MessageAccount>()
    const [personalAccount, setPersonalAccount] = useState<MessageAccount>()
    const [groupAccounts, setGroupAccounts] = useState<NestedMessageAccount[]>(
      []
    )

    useEffect(() => {
      if (nestedAccounts.isSuccess) {
        const groupAccounts = nestedAccounts.value.filter(
          ({ account, daycareGroup }) =>
            account.type === 'GROUP' && daycareGroup?.unitId === unitId
        )
        const maybePersonalAccount = nestedAccounts.value.find(
          (a) => a.account.type === 'PERSONAL'
        )?.account
        const maybeGroupAccount = groupAccounts[0]?.account
        setGroupAccounts(groupAccounts)
        setSelectedSender(maybePersonalAccount ?? maybeGroupAccount)
        if (maybePersonalAccount) setPersonalAccount(maybePersonalAccount)
      }
    }, [nestedAccounts, setSelectedSender, unitId])

    const [unreadCountsByAccount, setUnreadCountsByAccount] = useState<
      Result<UnreadCountByAccount[]>
    >(Loading.of())

    const loadUnreadCounts = useRestApi(
      getUnreadCounts,
      setUnreadCountsByAccount
    )

    const userHasActivePinLogin = user
      .map((item) => item?.pinLoginActive)
      .getOrElse(false)

    useEffect(() => {
      loggedIn && userHasActivePinLogin && unitId ? loadUnreadCounts() : null
    }, [loadUnreadCounts, user, userHasActivePinLogin, loggedIn, unitId])

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

    const loadMessagesForSelectedAccount = useCallback(() => {
      setReceivedMessages(Loading.of())
      if (selectedAccount)
        loadReceivedMessages(selectedAccount.id, page, PAGE_SIZE)
    }, [loadReceivedMessages, page, selectedAccount])

    const loadMessagesForAllAccounts = useCallback(() => {
      setSelectedAccount(undefined)
      void Promise.all(
        getAccountsByUserAndUnit(groupAccounts, personalAccount).map(
          (account) => getReceivedMessages(account.id, page, PAGE_SIZE)
        )
      ).then((result) => {
        const messages: MessageThread[] = result.flatMap(
          (item: Result<Paged<MessageThread>>) => {
            return item.isSuccess ? item.value.data : []
          }
        )
        setReceivedMessages(Success.of(messages))
      })
    }, [groupAccounts, personalAccount, page])

    const loadMessagesWhenGroupChanges = useCallback(
      (selectedGroup: GroupInfo | undefined): void => {
        console.log('loadMessagesWhenGroupChanges')
        if (selectedGroup === undefined) loadMessagesForAllAccounts()
        else {
          const maybeAccount =
            groupAccounts.find(({ account, daycareGroup }) =>
              selectedGroup
                ? daycareGroup?.id === selectedGroup.id
                : account.type === 'PERSONAL'
            )?.account ?? undefined

          setSelectedAccount(maybeAccount)
          loadMessagesForSelectedAccount()
        }
      },
      [
        groupAccounts,
        loadMessagesForAllAccounts,
        loadMessagesForSelectedAccount
      ]
    )

    const [selectedThread, setSelectedThread] = useState<MessageThread>()

    const selectThread = useCallback(
      (thread: MessageThread | undefined) => {
        setSelectedThread(thread)
        if (!thread) return
        if (!selectedSender) throw new Error('Should never happen')
        const { id: accountId } = selectedAccount || selectedSender

        const threadUnreadCount = getThreadUnreadCount(thread, accountId)
        if (threadUnreadCount > 0) {
          setUnreadCountsByAccount((request) =>
            request.map((result) =>
              adjustUnreadCounts(result, accountId, threadUnreadCount)
            )
          )
        }

        void markThreadRead(accountId, thread.id)
      },
      [selectedAccount, selectedSender, setUnreadCountsByAccount]
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
        selectedSender,
        selectedAccount,
        setSelectedAccount,
        groupAccounts,
        page,
        setPage,
        pages,
        setPages,
        receivedMessages,
        loadMessagesForAllAccounts,
        loadMessagesForSelectedAccount,
        loadMessagesWhenGroupChanges,
        personalAccount,
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
        groupAccounts,
        selectedAccount,
        selectedSender,
        page,
        pages,
        receivedMessages,
        loadMessagesForAllAccounts,
        loadMessagesForSelectedAccount,
        loadMessagesWhenGroupChanges,
        personalAccount,
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
