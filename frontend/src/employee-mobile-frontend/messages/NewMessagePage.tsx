// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'

import { UnitOrGroup } from 'employee-mobile-frontend/common/unit-or-group'
import { combine } from 'lib-common/api'
import { useQueryResult } from 'lib-common/query'

import { routes } from '../App'
import { renderResult } from '../async-rendering'

import MessageEditor from './MessageEditor'
import { recipientsQuery } from './queries'
import { MessageContext } from './state'

export default function NewMessagePage({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const { groupAccount } = useContext(MessageContext)
  const recipients = useQueryResult(recipientsQuery(), {
    enabled: unitOrGroup.type === 'group'
  })
  const navigate = useNavigate()

  const onClose = () => {
    navigate(routes.messages(unitOrGroup).value)
  }

  if (unitOrGroup.type === 'unit') {
    return <Navigate to={routes.messages(unitOrGroup).value} />
  }

  return renderResult(
    combine(groupAccount(unitOrGroup.id), recipients),
    ([selectedAccount, availableRecipients]) =>
      selectedAccount ? (
        <MessageEditor
          unitId={unitOrGroup.unitId}
          availableRecipients={availableRecipients}
          account={selectedAccount.account}
          draft={undefined}
          onClose={onClose}
        />
      ) : (
        <Navigate
          to={
            routes.messages({ type: 'unit', unitId: unitOrGroup.unitId }).value
          }
        />
      )
  )
}
