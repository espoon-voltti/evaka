// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import uniqBy from 'lodash/uniqBy'
import React, {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState
} from 'react'

import { Loading, Result } from 'lib-common/api'
import {
  MessageThread,
  AuthorizedMessageAccount
} from 'lib-common/generated/api-types/messaging'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import {
  queryResult,
  useInfiniteQuery,
  useMutation,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'

import { UserContext } from '../auth/state'
import { UnitContext } from '../common/unit'

import { ReplyToThreadParams } from './api'
import {
  markThreadReadMutation,
  messagingAccountsQuery,
  receivedMessagesQuery,
  replyToThreadMutation
} from './queries'

const PAGE_SIZE = 20

export interface MessagesState {
  accounts: Result<AuthorizedMessageAccount[]>
  groupAccounts: AuthorizedMessageAccount[]
  selectedAccount: AuthorizedMessageAccount | undefined
  receivedMessages: Result<MessageThread[]>
  selectedThread: MessageThread | undefined
  selectThread: (thread: MessageThread | undefined) => void
  sendReply: (params: ReplyToThreadParams) => void
  setReplyContent: (threadId: UUID, content: string) => void
  getReplyContent: (threadId: UUID) => string
}

const defaultState: MessagesState = {
  accounts: Loading.of(),
  selectedAccount: undefined,
  groupAccounts: [],
  receivedMessages: Loading.of(),
  selectedThread: undefined,
  selectThread: () => undefined,
  sendReply: () => undefined,
  getReplyContent: () => '',
  setReplyContent: () => undefined
}

export const MessageContext = createContext<MessagesState>(defaultState)

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
  function MessageContextProvider({ children }: { children: JSX.Element }) {
    const { unitInfoResponse } = useContext(UnitContext)
    const { user } = useContext(UserContext)
    const pinLoginActive = user.map((u) => u?.pinLoginActive).getOrElse(false)

    const unitId = unitInfoResponse.map((res) => res.id).getOrElse(undefined)

    const { groupId } = useNonNullableParams<{
      // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
      groupId: UUID | 'all'
    }>()

    const accounts = useQueryResult(messagingAccountsQuery(unitId ?? ''), {
      enabled: unitId !== undefined && pinLoginActive
    })

    const groupAccounts: AuthorizedMessageAccount[] = useMemo(
      () =>
        accounts
          .map((acc) =>
            acc.filter(
              ({ account, daycareGroup }) =>
                account.type === 'GROUP' && daycareGroup?.unitId === unitId
            )
          )
          .getOrElse([]),
      [accounts, unitId]
    )

    const selectedAccount: AuthorizedMessageAccount | undefined = useMemo(
      () =>
        groupAccounts.find(
          ({ daycareGroup }) => daycareGroup?.id === groupId
        ) ?? groupAccounts[0],
      [groupAccounts, groupId]
    )

    const { data, transformPages, error, isFetching, isFetchingNextPage } =
      useInfiniteQuery(
        receivedMessagesQuery(selectedAccount?.account.id ?? '', PAGE_SIZE),
        { enabled: selectedAccount !== undefined && pinLoginActive }
      )

    const isFetchingFirstPage = isFetching && !isFetchingNextPage
    const threads = useMemo(
      () =>
        // Use .map() to only call uniqBy/flatMap when it's a Success
        queryResult(null, error, isFetchingFirstPage).map(() =>
          data
            ? uniqBy(
                data.pages.flatMap((p) => p.data),
                'id'
              )
            : []
        ),
      [data, error, isFetchingFirstPage]
    )

    const [selectedThreadId, setSelectedThreadId] = useState<UUID>()

    const selectedThread = threads
      .map((threads) => threads.find((t) => t.id === selectedThreadId))
      .getOrElse(undefined)

    const { mutate: markThreadRead } = useMutation(markThreadReadMutation)

    const selectThread = useCallback(
      (thread: MessageThread | undefined) => {
        setSelectedThreadId(thread?.id)

        if (!thread) return
        if (!selectedAccount) throw new Error('Should never happen')
        const { id: accountId } = selectedAccount.account

        const hasUnreadMessages = thread.messages.some(
          (m) => !m.readAt && m.sender.id !== accountId
        )

        if (hasUnreadMessages) {
          markThreadRead({ accountId, id: thread.id })
          transformPages((page) => ({
            ...page,
            data: markMatchingThreadRead(page.data, thread.id)
          }))
        }
      },

      [markThreadRead, selectedAccount, transformPages]
    )

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
          setReplyContent(result.value.threadId, '')
        }
      },
      [sendReply, setReplyContent]
    )

    const value = useMemo(
      () => ({
        accounts,
        selectedAccount,
        groupAccounts,
        receivedMessages: threads,
        selectThread,
        selectedThread,
        getReplyContent,
        sendReply: sendReplyAndClear,
        setReplyContent
      }),
      [
        accounts,
        groupAccounts,
        selectedAccount,
        threads,
        selectedThread,
        selectThread,
        getReplyContent,
        sendReplyAndClear,
        setReplyContent
      ]
    )

    return (
      <MessageContext.Provider value={value}>
        {children}
      </MessageContext.Provider>
    )
  }
)
