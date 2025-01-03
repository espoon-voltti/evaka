// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useEffect, useState } from 'react'

import FiniteDateRange from 'lib-common/finite-date-range'
import {
  Message,
  MessageAccountWithPresence
} from 'lib-common/generated/api-types/messaging'
import { MessageAccountId } from 'lib-common/generated/api-types/shared'
import { UUID } from 'lib-common/types'

import { SelectableAccount } from '../messages/MessageReplyEditor'

function getOutOfOfficeForAccount(
  accountId: MessageAccountId,
  accounts: MessageAccountWithPresence[]
): FiniteDateRange | null {
  const account = accounts.find((acc) => acc.account.id === accountId)
  return account ? account.outOfOffice : null
}

function getInitialRecipients(
  threadMessages: Message[],
  replierAccountId: UUID,
  accountDetails: MessageAccountWithPresence[]
): SelectableAccount[] {
  const firstMessage = threadMessages[0]
  const lastMessage = threadMessages.slice(-1)[0]
  const lastRecipients = lastMessage.recipients.map(({ id }) => id)
  return [
    ...(firstMessage.sender.id !== replierAccountId
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
      .filter((r) => r.id !== replierAccountId)
      .map((acc) => ({
        ...acc,
        toggleable: true,
        selected:
          lastMessage.sender.id === acc.id || lastRecipients.includes(acc.id),
        outOfOffice: getOutOfOfficeForAccount(acc.id, accountDetails)
      }))
  ]
}

export function useRecipients(
  threadMessages: Message[],
  replierAccountId: MessageAccountId,
  accountDetails: MessageAccountWithPresence[] | null
) {
  const [recipients, setRecipients] = useState<SelectableAccount[]>([])

  useEffect(() => {
    setRecipients(
      getInitialRecipients(
        threadMessages,
        replierAccountId,
        accountDetails ?? []
      )
    )
  }, [threadMessages, replierAccountId, accountDetails])

  const onToggleRecipient = useCallback(
    (id: MessageAccountId, selected: boolean) => {
      setRecipients((prev) =>
        prev.map((acc) =>
          acc.id === id && acc.toggleable ? { ...acc, selected } : acc
        )
      )
    },
    []
  )
  return { recipients, onToggleRecipient }
}
