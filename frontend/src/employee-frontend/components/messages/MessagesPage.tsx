// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import ReceiverSelection from 'employee-frontend/components/messages/ReceiverSelection'
import {
  getReceiverOptions,
  getSelected,
  getSelectedBottomElements,
  SelectorChange,
  SelectorNode,
  unitAsSelectorNode,
  updateSelector
} from 'employee-frontend/components/messages/SelectorNode'
import { Loading, Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Container from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { getMessagingAccounts, getReceivers, postMessage } from './api'
import MessageEditor from './MessageEditor'
import MessageList from './MessageList'
import Sidebar from './Sidebar'
import { Message, MessageAccount, ReceiverGroup } from './types'
import { AccountView } from './types-view'

const PanelContainer = styled.div`
  display: flex;
`

export default React.memo(function MessagesPage() {
  const [accounts, setResult] = useState<Result<MessageAccount[]>>(Loading.of())
  const loadAccounts = useRestApi(getMessagingAccounts, setResult)
  useEffect(() => loadAccounts(), [loadAccounts])
  const [receivers, setReceivers] = useState<Result<ReceiverGroup[]>>(
    Loading.of()
  )
  const loadReceivers = useRestApi(getReceivers, setReceivers)
  const [selectedReceivers, setSelectedReceivers] = useState<SelectorNode>()
  const [selectorChange, setSelectorChange] = useState<SelectorChange>()

  const [view, setView] = useState<AccountView>()
  const [unitStuff, setUnitStuff] = useState<{
    unitId: string
    unitName: string
  }>()
  const [showEditor, setShowEditor] = useState<boolean>(false)
  const [message, setMessage] = useState<Message>({
    title: '',
    receivers: [],
    content: '',
    id: '',
    senderId: '',
    senderName: '',
    sentAt: new Date(),
    readAt: new Date()
  })

  useEffect(() => {
    if (view?.account.daycareGroup) {
      const { unitId, unitName } = view.account.daycareGroup
      setUnitStuff({ unitId, unitName })
    }
  }, [view])

  useEffect(() => {
    if (
      receivers.isSuccess &&
      unitStuff &&
      selectedReceivers?.selectorId !== unitStuff.unitId
    ) {
      setSelectedReceivers(
        unitAsSelectorNode(
          { id: unitStuff.unitId, name: unitStuff.unitName },
          receivers.value
        )
      )
    }
  }, [unitStuff, receivers, selectedReceivers?.selectorId])

  useEffect(() => {
    if (view) {
      view.account.daycareGroup &&
        loadReceivers(view.account.daycareGroup.unitId)
    }
  }, [view, loadReceivers])

  useEffect(() => {
    if (accounts.isSuccess && accounts.value[0]) {
      setView({ view: 'RECEIVED', account: accounts.value[0] })
    }
  }, [accounts])

  useEffect(() => {
    if (selectorChange) {
      setSelectedReceivers((selected: SelectorNode | undefined) =>
        selected ? updateSelector(selected, selectorChange) : undefined
      )
    }
  }, [selectorChange])

  useEffect(() => {
    if (selectedReceivers) {
      setMessage((oldMessage) => ({
        ...oldMessage,
        receivers: getSelectedBottomElements(selectedReceivers)
      }))
    }
  }, [selectedReceivers])

  return (
    <Container>
      <Gap size="L" />
      <PanelContainer>
        <Sidebar
          accounts={accounts}
          view={view}
          setView={setView}
          showEditor={() => setShowEditor(true)}
        />
        {view && (view.view === 'RECEIVED' || view.view === 'SENT') && (
          <MessageList {...view} />
        )}
        {view && view.view === 'RECEIVERS' && selectedReceivers && (
          <ReceiverSelection
            selectedReceivers={selectedReceivers}
            setSelectorChange={setSelectorChange}
          />
        )}
        {showEditor && accounts.isSuccess && (
          <MessageEditor
            message={message}
            onChange={(message: Message) => setMessage(message)}
            onClose={() => setShowEditor(false)}
            onSend={() =>
              postMessage({
                title: message.title,
                content: message.content,
                type: 'MESSAGE',
                senderAccountId: accounts.value[0].id,
                recipientAccountIds: message.receivers
              })
            }
            selectedReceivers={
              selectedReceivers ? getSelected(selectedReceivers) : []
            }
            receiverOptions={
              selectedReceivers ? getReceiverOptions(selectedReceivers) : []
            }
            setSelectorChange={setSelectorChange}
          />
        )}
      </PanelContainer>
    </Container>
  )
})
