// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Navigate } from 'react-router-dom'
import { useNavigate } from 'react-router-dom'

import useRouteParams from 'lib-common/useRouteParams'
import { ContentArea } from 'lib-components/layout/Container'

import { routes } from '../App'
import { UnitOrGroup } from '../common/unit-or-group'

import { ReceivedThreadView } from './ThreadView'
import { MessageContext } from './state'

export default function ReceivedThreadPage({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const { threadId } = useRouteParams(['threadId'])
  const navigate = useNavigate()
  const { selectedAccount } = useContext(MessageContext)

  const onBack = () => {
    navigate(routes.messages(unitOrGroup).value)
  }

  if (!selectedAccount) {
    return <Navigate to={routes.messages(unitOrGroup).value} replace={true} />
  }
  return (
    <ContentArea
      opaque={false}
      fullHeight
      paddingHorizontal="zero"
      paddingVertical="zero"
      data-qa="messages-page-content-area"
    >
      <ReceivedThreadView
        unitId={unitOrGroup.unitId}
        threadId={threadId}
        onBack={onBack}
        accountId={selectedAccount.account.id}
      />
    </ContentArea>
  )
}
