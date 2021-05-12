// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Loading, Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Container from 'lib-components/layout/Container'
import React, { useEffect, useState } from 'react'
import { getMessagingAccounts } from './api'
import MessageList from './MessageList'
import Sidebar from './Sidebar'
import { EnrichedMessageAccount } from './types'

export default React.memo(function MessagesPage() {
  const [accounts, setResult] = useState<Result<EnrichedMessageAccount[]>>(
    Loading.of()
  )
  const loadAccounts = useRestApi(getMessagingAccounts, setResult)
  useEffect(() => loadAccounts(), [loadAccounts])

  const [selectedAccount, setSelectedAccount] = useState<UUID>()

  return (
    <Container>
      <Sidebar
        accounts={accounts}
        selectedAccount={selectedAccount}
        setSelectedAccount={setSelectedAccount}
      />
      {selectedAccount && (
        <MessageList view="RECEIVED" accountId={selectedAccount} />
      )}
    </Container>
  )
})
