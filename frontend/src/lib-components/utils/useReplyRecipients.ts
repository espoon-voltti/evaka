// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useState } from 'react'

import type FiniteDateRange from 'lib-common/finite-date-range'
import type {
  Message,
  MessageAccountWithPresence
} from 'lib-common/generated/api-types/messaging'
import type { MessageAccountId } from 'lib-common/generated/api-types/shared'
import type { TypedMessageAccount } from 'lib-common/messaging'

import type { SelectableAccount } from '../messages/MessageReplyEditor'

function getOutOfOfficeForAccount(
  accountId: MessageAccountId,
  accounts: MessageAccountWithPresence[]
): FiniteDateRange | null {
  const account = accounts.find((acc) => acc.account.id === accountId)
  return account ? account.outOfOffice : null
}

function getInitialRecipients(
  threadMessages: Message[],
  replierAccount: TypedMessageAccount,
  accountDetails: MessageAccountWithPresence[]
): SelectableAccount[] {
  const firstMessage = threadMessages[0]
  const lastMessage = threadMessages.slice(-1)[0]
  const lastRecipients = lastMessage.recipients.map(({ id }) => id)
  return [
    ...(firstMessage.sender.id !== replierAccount.id
      ? [
          {
            ...firstMessage.sender,
            toggleable: false,
            selected: true,
            outOfOffice: getOutOfOfficeForAccount(
              firstMessage.sender.id,
              accountDetails
            )
          }
        ]
      : []),
    ...firstMessage.recipients
      .filter((r) => r.id !== replierAccount.id)
      .map((acc) => ({
        ...acc,
        toggleable: true,
        selected:
          lastMessage.sender.id === acc.id ||
          ((replierAccount.type !== 'CITIZEN' || acc.type !== 'CITIZEN') &&
            lastRecipients.includes(acc.id)),
        outOfOffice: getOutOfOfficeForAccount(acc.id, accountDetails)
      }))
  ]
}

export function useRecipients(
  threadMessages: Message[],
  replierAccount: TypedMessageAccount,
  accountDetails: MessageAccountWithPresence[] | null
) {
  const [state, setState] = useState<{
    recipients: SelectableAccount[]
    prevThreadMessages: Message[]
    prevReplierAccount: TypedMessageAccount
    prevAccountDetails: MessageAccountWithPresence[] | null
  }>(() => ({
    recipients: getInitialRecipients(
      threadMessages,
      replierAccount,
      accountDetails ?? []
    ),
    prevThreadMessages: threadMessages,
    prevReplierAccount: replierAccount,
    prevAccountDetails: accountDetails
  }))

  if (
    state.prevThreadMessages !== threadMessages ||
    state.prevReplierAccount !== replierAccount ||
    state.prevAccountDetails !== accountDetails
  ) {
    setState({
      recipients: getInitialRecipients(
        threadMessages,
        replierAccount,
        accountDetails ?? []
      ),
      prevThreadMessages: threadMessages,
      prevReplierAccount: replierAccount,
      prevAccountDetails: accountDetails
    })
  }

  const onToggleRecipient = useCallback(
    (id: MessageAccountId, selected: boolean) => {
      setState((prev) => ({
        ...prev,
        recipients: prev.recipients.map((acc) =>
          acc.id === id && acc.toggleable ? { ...acc, selected } : acc
        )
      }))
    },
    []
  )
  return { recipients: state.recipients, onToggleRecipient }
}
