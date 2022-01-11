// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import { PostMessageBody } from 'lib-common/generated/api-types/messaging'
import { UUID } from 'lib-common/types'
import MessageEditor from 'lib-components/employee/messages/MessageEditor'
import {
  deselectAll,
  SelectorNode
} from 'lib-components/employee/messages/SelectorNode'
import Container from 'lib-components/layout/Container'
import { defaultMargins } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'
import {
  deleteAttachment,
  getAttachmentBlob,
  saveMessageAttachment
} from '../../api/attachments'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { footerHeight } from '../Footer'
import { headerHeight } from '../Header'
import { MessageContext } from './MessageContext'
import ReceiverSelection from './ReceiverSelection'
import Sidebar from './Sidebar'
import MessageList from './ThreadListContainer'
import { deleteDraft, initDraft, postMessage, saveDraft } from './api'

const PanelContainer = styled.div`
  height: calc(
    100vh - ${headerHeight} - ${footerHeight} - ${defaultMargins.XL}
  );
  display: flex;
`

export default function MessagesPage() {
  const {
    accounts,
    selectedDraft,
    setSelectedDraft,
    selectedAccount,
    setSelectedAccount,
    selectedUnit,
    refreshMessages
  } = useContext(MessageContext)

  const { setErrorMessage } = useContext(UIContext)
  const { i18n } = useTranslation()

  useEffect(() => refreshMessages(), [refreshMessages])
  const [sending, setSending] = useState(false)

  // pre-select first account on page load and on unit change
  useEffect(() => {
    if (!accounts.isSuccess) {
      return
    }
    const { value: data } = accounts
    const unitSelectionChange =
      selectedAccount &&
      !data.find((acc) => acc.account.id === selectedAccount.account.id)
    if ((!selectedAccount || unitSelectionChange) && data.length > 0) {
      setSelectedAccount({
        view: 'RECEIVED',
        account:
          data.find((a) => a.account.type === 'PERSONAL')?.account ||
          data[0].account
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

  const hideEditor = useCallback(() => {
    setSelectedReceivers((old) => (old ? deselectAll(old) : old))
    setShowEditor(false)
    setSelectedDraft(undefined)
  }, [setSelectedDraft])

  const onSend = useCallback(
    (accountId: UUID, messageBody: PostMessageBody) => {
      setSending(true)
      void postMessage(accountId, messageBody).then((res) => {
        if (res.isSuccess) {
          refreshMessages(accountId)
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

  const onDiscard = (accountId: UUID, draftId: UUID) => {
    hideEditor()
    void deleteDraft(accountId, draftId).then(() => refreshMessages(accountId))
  }

  const onHide = (didChanges: boolean) => {
    hideEditor()
    if (didChanges) {
      refreshMessages()
    }
  }

  return (
    <Container>
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
          selectedAccount &&
          selectedUnit && (
            <MessageEditor
              availableReceivers={selectedReceivers}
              attachmentsEnabled={
                featureFlags.experimental?.messageAttachments ?? false
              }
              defaultSender={{
                value: selectedAccount.account.id,
                label: selectedAccount.account.name
              }}
              deleteAttachment={deleteAttachment}
              draftContent={selectedDraft}
              getAttachmentBlob={getAttachmentBlob}
              i18n={{
                ...i18n.messages.messageEditor,
                ...i18n.fileUpload,
                ...i18n.common
              }}
              initDraftRaw={initDraft}
              accounts={accounts.value}
              onClose={onHide}
              onDiscard={onDiscard}
              onSend={onSend}
              saveDraftRaw={saveDraft}
              saveMessageAttachment={saveMessageAttachment}
              selectedUnit={selectedUnit}
              sending={sending}
            />
          )}
      </PanelContainer>
    </Container>
  )
}
