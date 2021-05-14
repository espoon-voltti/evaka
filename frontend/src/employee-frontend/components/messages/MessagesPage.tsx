// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Loading, Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Container from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { getMessagingAccounts } from './api'
import MessageEditor from './MessageEditor'
import MessageList from './MessageList'
import Sidebar from './Sidebar'
import { Message, MessageAccount } from './types'

const PanelContainer = styled.div`
  display: flex;
`

export default React.memo(function MessagesPage() {
  const [accounts, setResult] = useState<Result<MessageAccount[]>>(Loading.of())
  const loadAccounts = useRestApi(getMessagingAccounts, setResult)
  useEffect(() => loadAccounts(), [loadAccounts])

  const [selectedAccount, setSelectedAccount] = useState<MessageAccount>()
  const [showEditor, setShowEditor] = useState<boolean>(false)
  const [message, setMessage] = useState<Message>({
    title: '',
    receivers: '',
    content: '',
    id: '',
    senderId: '',
    senderName: '',
    sentAt: new Date(),
    readAt: new Date()
  })

  return (
    <Container>
      <Gap size="L" />
      <PanelContainer>
        <Sidebar
          accounts={accounts}
          selectedAccount={selectedAccount}
          setSelectedAccount={setSelectedAccount}
          showEditor={() => setShowEditor(true)}
        />
        {selectedAccount && (
          <MessageList view="RECEIVED" account={selectedAccount} />
        )}
        {showEditor && (
          <MessageEditor
            message={message}
            onChange={(message: Message) => setMessage(message)}
            onClose={() => setShowEditor(false)}
            onSend={() => {}}
          />
        )}
      </PanelContainer>
    </Container>
  )
})
