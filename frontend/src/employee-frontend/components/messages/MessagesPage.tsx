// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Loading, Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Container from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import { deleteDraft, getMessagingAccounts, postMessage } from './api'
import MessageEditor from './MessageEditor'
import MessageList from './MessageList'
import {
  MessagesPageContext,
  MessagesPageContextProvider
} from './MessagesPageContext'
import ReceiverSelection from './ReceiverSelection'
import { deselectAll, SelectorNode } from './SelectorNode'
import Sidebar from './Sidebar'
import { MessageAccount, MessageBody } from './types'

const PanelContainer = styled.div`
  display: flex;
`

function MessagesPage() {
  const {
    selectedDraft,
    setSelectedDraft,
    view,
    setView,
    refreshMessages
  } = useContext(MessagesPageContext)

  const [accounts, setResult] = useState<Result<MessageAccount[]>>(Loading.of())
  const loadAccounts = useRestApi(getMessagingAccounts, setResult)
  useEffect(() => loadAccounts(), [loadAccounts])

  const [selectedReceivers, setSelectedReceivers] = useState<SelectorNode>()

  const [showEditor, setShowEditor] = useState<boolean>(false)
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

  // open editor when draft is selected
  useEffect(() => {
    if (selectedDraft) {
      setShowEditor(true)
    }
  }, [selectedDraft])

  useEffect(() => {
    if (accounts.isSuccess && accounts.value[0]) {
      setView({
        view: 'RECEIVED',
        account: accounts.value[0]
      })
    }
  }, [accounts, setView])

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
            defaultSender={{ value: view.account.id, label: view.account.name }}
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
    </Container>
  )
}

export default function MessagesPageWrapper() {
  return (
    <MessagesPageContextProvider>
      <MessagesPage />
    </MessagesPageContextProvider>
  )
}
