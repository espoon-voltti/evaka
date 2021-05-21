// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import ReceiverSelection from 'employee-frontend/components/messages/ReceiverSelection'
import {
  deselectAll,
  getReceiverOptions,
  SelectorNode
} from 'employee-frontend/components/messages/SelectorNode'
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
import { MessageAccount } from './types'
import { AccountView } from './types-view'

const PanelContainer = styled.div`
  display: flex;
`

export default React.memo(function MessagesPage() {
  const [accounts, setResult] = useState<Result<MessageAccount[]>>(Loading.of())
  const loadAccounts = useRestApi(getMessagingAccounts, setResult)
  useEffect(() => loadAccounts(), [loadAccounts])
  const [view, setView] = useState<AccountView>()

  const [selectedReceivers, setSelectedReceivers] = useState<SelectorNode>()

  const [showEditor, setShowEditor] = useState<boolean>(false)
  const hideEditor = () => {
    selectedReceivers && setSelectedReceivers(deselectAll(selectedReceivers))
    setShowEditor(false)
  }

  useEffect(() => {
    if (accounts.isSuccess && accounts.value[0]) {
      setView({
        view: 'RECEIVED',
        account: accounts.value[0]
      })
    }
  }, [accounts])

  return (
    <Container>
      <Gap size="L" />
      <PanelContainer>
        <Sidebar
          accounts={accounts}
          view={view}
          setView={setView}
          setSelectedReceivers={setSelectedReceivers}
          showEditor={() => setShowEditor(true)}
        />
        {(view?.view === 'RECEIVED' ||
          view?.view === 'SENT' ||
          view?.view === 'DRAFTS') && <MessageList {...view} />}
        {view?.view === 'RECEIVERS' && selectedReceivers && (
          <ReceiverSelection
            selectedReceivers={selectedReceivers}
            setSelectedReceivers={setSelectedReceivers}
          />
        )}
        {showEditor && accounts.isSuccess && selectedReceivers && view && (
          <MessageEditor
            defaultAccountSelection={{
              value: view.account.id,
              label: view.account.id
            }}
            accountOptions={accounts.value.map(({ name, id }) => ({
              value: id,
              label: name
            }))}
            selectedReceivers={selectedReceivers}
            receiverOptions={
              selectedReceivers ? getReceiverOptions(selectedReceivers) : []
            }
            setSelectedReceivers={setSelectedReceivers}
            hideEditor={hideEditor}
          />
        )}
      </PanelContainer>
    </Container>
  )
})
