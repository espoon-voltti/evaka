// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import Container from 'lib-components/layout/Container'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import { defaultMargins } from 'lib-components/white-space'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { deleteDraft, postMessage } from './api'
import { MessageContext } from './MessageContext'
import MessageEditor from './MessageEditor'
import MessageList from './ThreadListContainer'
import { deselectAll, SelectorNode } from './SelectorNode'
import Sidebar from './Sidebar'
import { MessageBody } from './types'
import ReceiverSelection from './ReceiverSelection'

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
    nestedAccounts,
    loadNestedAccounts,
    selectedDraft,
    setSelectedDraft,
    selectedAccount,
    setSelectedAccount,
    selectedUnit,
    refreshMessages
  } = useContext(MessageContext)

  const { setErrorMessage } = useContext(UIContext)
  const { i18n } = useTranslation()

  useEffect(() => loadNestedAccounts(), [loadNestedAccounts])
  useEffect(() => refreshMessages(), [refreshMessages])
  const [sending, setSending] = useState(false)

  // pre-select first account on page load and on unit change
  useEffect(() => {
    if (!nestedAccounts.isSuccess) {
      return
    }
    const { value: data } = nestedAccounts
    const unitSelectionChange =
      selectedAccount &&
      !data.find(
        (nestedAccount) =>
          nestedAccount.account.id === selectedAccount.account.id
      )
    if ((!selectedAccount || unitSelectionChange) && data.length > 0) {
      setSelectedAccount({
        view: 'RECEIVED',
        account:
          data.find((a) => a.account.type === 'PERSONAL')?.account ||
          data[0].account
      })
    }
  }, [nestedAccounts, setSelectedAccount, selectedAccount])

  const [showEditor, setShowEditor] = useState<boolean>(false)

  // open editor when draft is selected
  useEffect(() => {
    if (selectedDraft) {
      setShowEditor(true)
    }
  }, [selectedDraft])

  const [selectedReceivers, setSelectedReceivers] = useState<SelectorNode>()

  const hideEditor = useCallback(() => {
    setSelectedReceivers((old) => (old ? deselectAll(old) : old))
    setShowEditor(false)
    setSelectedDraft(undefined)
  }, [setSelectedDraft])

  const onSend = useCallback(
    (accountId: UUID, messageBody: MessageBody, draftId?: UUID) => {
      setSending(true)
      void postMessage(accountId, messageBody).then((res) => {
        if (res.isSuccess) {
          const maybeDeleteDraft = draftId
            ? deleteDraft(accountId, draftId)
            : Promise.resolve()
          void maybeDeleteDraft.then(() => refreshMessages(accountId))
          hideEditor()
        } else {
          setErrorMessage({
            type: 'error',
            title: i18n.common.error.unknown,
            resolveLabel: i18n.common.ok
          })
        }
        setSending(false)
      })
    },
    [hideEditor, i18n, refreshMessages, setErrorMessage]
  )

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
          nestedAccounts.isSuccess &&
          selectedReceivers &&
          selectedAccount &&
          selectedUnit && (
            <MessageEditor
              defaultSender={{
                value: selectedAccount.account.id,
                label: selectedAccount.account.name
              }}
              nestedAccounts={nestedAccounts.value}
              selectedUnit={selectedUnit}
              availableReceivers={selectedReceivers}
              onSend={onSend}
              sending={sending}
              onDiscard={onDiscard}
              onClose={onHide}
              draftContent={selectedDraft}
            />
          )}
      </PanelContainer>
    </MessagesPageContainer>
  )
}
