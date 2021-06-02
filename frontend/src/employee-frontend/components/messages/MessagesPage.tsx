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
import { MessageContext } from './MessageContext'
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

export default function MessagesPage() {
  const {
    accountsRequest,
    loadAccounts,
    selectedDraft,
    setSelectedDraft,
    selectedAccount,
    setSelectedAccount,
    selectedUnit,
    refreshMessages
  } = useContext(MessageContext)

  useEffect(() => loadAccounts(), [loadAccounts])
  useEffect(() => refreshMessages(), [refreshMessages])

  // pre-select first account on page load and on unit change
  useEffect(() => {
    const unitSelectionChange =
      selectedAccount &&
      accountsRequest.isSuccess &&
      !accountsRequest.value.find(
        (account) => account.id === selectedAccount.account.id
      )
    if (
      (!selectedAccount || unitSelectionChange) &&
      accountsRequest.isSuccess &&
      accountsRequest.value[0]
    ) {
      setSelectedAccount({
        view: 'RECEIVED',
        account: accountsRequest.value[0]
      })
    }
  }, [accountsRequest, setSelectedAccount, selectedAccount])

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
    void postMessage(accountId, messageBody)
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
          accountsRequest.isSuccess &&
          selectedReceivers &&
          selectedAccount &&
          selectedUnit && (
            <MessageEditor
              defaultSender={{
                value: selectedAccount.account.id,
                label: selectedAccount.account.name
              }}
              accounts={accountsRequest.value}
              selectedUnit={selectedUnit}
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
