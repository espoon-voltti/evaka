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
import { queryOrDefault, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { UserContext } from '../auth/state'
import { UnitOrGroup } from '../common/unit-or-group'

import { messagingAccountsQuery } from './queries'

export interface MessagesState {
  groupAccounts: Result<AuthorizedMessageAccount[]>
  groupAccount: (
    groupId: UUID | null
  ) => Result<AuthorizedMessageAccount | undefined>
  setReplyContent: (threadId: UUID, content: string) => void
  getReplyContent: (threadId: UUID) => string
}

const defaultState: MessagesState = {
  groupAccounts: Loading.of(),
  groupAccount: () => Loading.of(),
  getReplyContent: () => '',
  setReplyContent: () => undefined
}

export const MessageContext = createContext<MessagesState>(defaultState)

export const MessageContextProvider = React.memo(
  function MessageContextProvider({
    unitOrGroup,
    children
  }: {
    unitOrGroup: UnitOrGroup
    children: React.JSX.Element
  }) {
    const unitId = unitOrGroup.unitId
    const { user } = useContext(UserContext)
    const pinLoggedEmployeeId = user
      .map((u) =>
        u && u.pinLoginActive ? u.employeeId ?? undefined : undefined
      )
      .getOrElse(undefined)
    const shouldFetch = !!unitId && !!pinLoggedEmployeeId

    const groupAccounts = useQueryResult(
      queryOrDefault(
        messagingAccountsQuery,
        []
      )(shouldFetch ? { unitId, employeeId: pinLoggedEmployeeId } : undefined)
    )

    const groupAccount = useCallback(
      (groupId: UUID | null) =>
        groupAccounts.map((accounts) =>
          groupId
            ? accounts.find((account) => account.daycareGroup?.id === groupId)
            : undefined
        ),
      [groupAccounts]
    )

    const [replyContents, setReplyContents] = useState<Record<UUID, string>>({})

    const getReplyContent = useCallback(
      (threadId: UUID) => replyContents[threadId] ?? '',
      [replyContents]
    )
    const setReplyContent = useCallback((threadId: UUID, content: string) => {
      setReplyContents((state) => ({ ...state, [threadId]: content }))
    }, [])

    const value = useMemo(
      () => ({
        groupAccounts,
        groupAccount,
        getReplyContent,
        setReplyContent
      }),
      [groupAccounts, groupAccount, getReplyContent, setReplyContent]
    )

    return (
      <MessageContext.Provider value={value}>
        {children}
      </MessageContext.Provider>
    )
  }
)
