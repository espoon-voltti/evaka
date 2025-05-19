// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Navigate, useNavigate } from 'react-router'

import { combine } from 'lib-common/api'
import { useQueryResult } from 'lib-common/query'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import type { UnitOrGroup } from '../common/unit-or-group'
import { toUnitOrGroup } from '../common/unit-or-group'

import MessageEditor from './MessageEditor'
import { selectableRecipientsQuery } from './queries'
import { MessageContext } from './state'

export default function NewMessagePage({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const { groupAccount } = useContext(MessageContext)
  const recipients = useQueryResult(selectableRecipientsQuery(), {
    enabled: unitOrGroup.type === 'group'
  })
  const navigate = useNavigate()

  const onClose = () => {
    void navigate(routes.messages(unitOrGroup).value)
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
          to={routes.messages(toUnitOrGroup(unitOrGroup.unitId)).value}
        />
      )
  )
}
