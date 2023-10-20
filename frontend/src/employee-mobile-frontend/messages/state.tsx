// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState
} from 'react'

import { Loading, Result } from 'lib-common/api'
import { AuthorizedMessageAccount } from 'lib-common/generated/api-types/messaging'
import {
  queryOrDefault,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { UserContext } from '../auth/state'
import { useSelectedGroup } from '../common/selected-group'
import { UnitContext } from '../common/unit'

import { ReplyToThreadParams } from './api'
import { messagingAccountsQuery, replyToThreadMutation } from './queries'

export interface MessagesState {
  accounts: Result<AuthorizedMessageAccount[]>
  groupAccounts: AuthorizedMessageAccount[]
  selectedAccount: AuthorizedMessageAccount | undefined
  sendReply: (params: ReplyToThreadParams) => Promise<Result<unknown>>
  setReplyContent: (threadId: UUID, content: string) => void
  getReplyContent: (threadId: UUID) => string
}

const defaultState: MessagesState = {
  accounts: Loading.of(),
  selectedAccount: undefined,
  groupAccounts: [],
  sendReply: () => Promise.resolve(Loading.of()),
  getReplyContent: () => '',
  setReplyContent: () => undefined
}

export const MessageContext = createContext<MessagesState>(defaultState)

export const MessageContextProvider = React.memo(
  function MessageContextProvider({
    children
  }: {
    children: React.JSX.Element
  }) {
    const { unitInfoResponse } = useContext(UnitContext)

    const { user } = useContext(UserContext)
    const pinLoggedEmployeeId = user
      .map((u) => u?.employeeId ?? undefined)
      .getOrElse(undefined)

    const unitId = unitInfoResponse.map((res) => res.id).getOrElse(undefined)

    const { selectedGroupId } = useSelectedGroup()

    const accounts = useQueryResult(
      queryOrDefault(
        messagingAccountsQuery,
        []
      )(
        unitId && pinLoggedEmployeeId
          ? { unitId, employeeId: pinLoggedEmployeeId }
          : undefined
      )
    )

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
        (selectedGroupId.type === 'all'
          ? undefined
          : groupAccounts.find(
              ({ daycareGroup }) => daycareGroup?.id === selectedGroupId.id
            )) ?? groupAccounts[0],
      [groupAccounts, selectedGroupId]
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
        return result
      },
      [sendReply, setReplyContent]
    )

    const value = useMemo(
      () => ({
        accounts,
        selectedAccount,
        groupAccounts,
        getReplyContent,
        sendReply: sendReplyAndClear,
        setReplyContent
      }),
      [
        accounts,
        groupAccounts,
        selectedAccount,
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
