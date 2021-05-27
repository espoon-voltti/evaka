// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import Container from 'lib-components/layout/Container'
import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import { defaultMargins } from '../../../lib-components/white-space'
import { deleteDraft, postMessage } from './api'
import MessageEditor from './MessageEditor'
import MessageList from './MessageList'
import {
  MessagesPageContext,
  MessagesPageContextProvider
} from './MessagesPageContext'
import ReceiverSelection from './ReceiverSelection'
import { deselectAll, SelectorNode } from './SelectorNode'
import Sidebar from './Sidebar'
import { MessageBody } from './types'

// TODO is fixed header height possible?
// If not, replace with a stretching flex container with scrollable children
const approximatedHeaderHeight = `164px`
const MessagesPageContainer = styled(Container)`
  height: calc(100vh - ${approximatedHeaderHeight});
`

const PanelContainer = styled.div`
  position: absolute;
  top: ${defaultMargins.L};
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
`

function MessagesPage() {
  const {
    accounts,
    loadAccounts,
    selectedDraft,
    setSelectedDraft,
    selectedAccount,
    setSelectedAccount,
    refreshMessages
  } = useContext(MessagesPageContext)

  useEffect(() => loadAccounts(), [loadAccounts])

  // pre-select first account
  useEffect(() => {
    if (!selectedAccount && accounts.isSuccess && accounts.value[0]) {
      setSelectedAccount({
        view: 'RECEIVED',
        account: accounts.value[0]
      })
    }
  }, [accounts, setSelectedAccount, selectedAccount])

  const [showEditor, setShowEditor] = useState<boolean>(false)

  // open editor when draft is selected
  useEffect(() => {
    if (selectedDraft) {
      setShowEditor(true)
    }
  }, [selectedDraft])

  const [selectedReceivers, setSelectedReceivers] = useState<SelectorNode>()

  const hideEditor = () => {
    setSelectedReceivers((old) => (old ? deselectAll(old) : old))
    setShowEditor(false)
    setSelectedDraft(undefined)
  }

  const onSend = (
    accountId: UUID,
    messageBody: MessageBody,
    draftId?: UUID
  ) => {
    // TODO state and error handling
    void postMessage(accountId, {
      title: messageBody.title,
      content: messageBody.content,
      type: messageBody.type,
      recipientAccountIds: messageBody.recipientAccountIds
    })
      .then(() => {
        if (draftId) {
          void deleteDraft(accountId, draftId)
        }
      })
      .finally(() => {
        refreshMessages(accountId)
        hideEditor()
      })
  }

  const onDiscard = (accountId: UUID, draftId?: UUID) => {
    hideEditor()
    if (draftId) {
      void deleteDraft(accountId, draftId).then(() =>
        refreshMessages(accountId)
      )
    }
  }

  const onHide = (didChanges: boolean) => {
    hideEditor()
    if (didChanges) {
      refreshMessages()
    }
  }

  return (
    <MessagesPageContainer>
      <PanelContainer>
        <Sidebar
          setSelectedReceivers={setSelectedReceivers}
          showEditor={() => setShowEditor(true)}
        />
        {(selectedAccount?.view === 'RECEIVED' ||
          selectedAccount?.view === 'SENT' ||
          selectedAccount?.view === 'DRAFTS') && (
          <MessageList {...selectedAccount} />
        )}
        {selectedAccount?.view === 'RECEIVERS' && selectedReceivers && (
          <ReceiverSelection
            selectedReceivers={selectedReceivers}
            setSelectedReceivers={setSelectedReceivers}
          />
        )}
        {showEditor &&
          accounts.isSuccess &&
          selectedReceivers &&
          selectedAccount && (
            <MessageEditor
              defaultSender={{
                value: selectedAccount.account.id,
                label: selectedAccount.account.name
              }}
              senderOptions={accounts.value.map(({ name, id }) => ({
                value: id,
                label: name
              }))}
              availableReceivers={selectedReceivers}
              onSend={onSend}
              onDiscard={onDiscard}
              onClose={onHide}
              draftContent={selectedDraft}
            />
          )}
      </PanelContainer>
    </MessagesPageContainer>
  )
}

export default function MessagesPageWrapper() {
  return (
    <MessagesPageContextProvider>
      <MessagesPage />
    </MessagesPageContextProvider>
  )
}
