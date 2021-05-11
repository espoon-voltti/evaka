// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import MessageList from 'employee-frontend/components/messages/MessageList'
import Sidebar from 'employee-frontend/components/messages/Sidebar'
import Container from 'lib-components/layout/Container'
import React, { useEffect, useState } from 'react'
import { getMessagingAccounts } from 'employee-frontend/components/messages/api'
import { EnrichedMessageAccount } from 'employee-frontend/components/messages/types'
import { Result, Loading } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { UUID } from 'employee-frontend/types'

export default React.memo(function MessagesPage() {

  const [accounts, setResult] = useState<Result<EnrichedMessageAccount[]>>(
    Loading.of()
  )
  const messagingAccounts = useRestApi(getMessagingAccounts, setResult)
  useEffect(() => messagingAccounts(), [])

  const [selectedAccount, setSelectedAccount] = useState<UUID>()

  return (
    <Container>
      <Sidebar accounts={accounts} selectedAccount={selectedAccount} setSelectedAccount={setSelectedAccount} />
      <MessageList/>
      <MessageList/>
    </Container>
  )
})