// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'

import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { routes } from '../App'
import { renderResult } from '../async-rendering'

import MessageEditor from './MessageEditor'
import { recipientsQuery } from './queries'
import { MessageContext } from './state'

export default function NewMessagePage({ unitId }: { unitId: UUID }) {
  const { selectedAccount, done } = useContext(MessageContext)
  const recipients = useQueryResult(recipientsQuery(), {
    enabled: selectedAccount !== undefined
  })
  const navigate = useNavigate()

  const onClose = () => {
    navigate(routes.messages({ unitId, type: 'unit' }).value)
  }

  if (!done) {
    return null
  }
  if (!selectedAccount) {
    return <Navigate to={routes.messages({ unitId, type: 'unit' }).value} />
  }
  return renderResult(recipients, (availableRecipients) => (
    <MessageEditor
      unitId={unitId}
      availableRecipients={availableRecipients}
      account={selectedAccount.account}
      draft={undefined}
      onClose={onClose}
    />
  ))
}
