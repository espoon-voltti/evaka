// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import type { MessageReceiver } from 'lib-common/api-types/messaging'
import type { PostMessageBody } from 'lib-common/generated/api-types/messaging'
import type { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { useApiState } from 'lib-common/utils/useRestApi'
import MessageEditor from 'lib-components/employee/messages/MessageEditor'
import { ContentArea } from 'lib-components/layout/Container'
import { defaultMargins } from 'lib-components/white-space'
import { faArrowLeft } from 'lib-icons'

import { renderResult } from '../async-rendering'
import TopBar from '../common/TopBar'
import { BackButtonInline } from '../common/components'
import { useTranslation } from '../common/i18n'

import {
  deleteAttachment,
  deleteDraft,
  getAttachmentUrl,
  getReceivers,
  initDraft,
  postMessage,
  saveDraft,
  saveMessageAttachment
} from './api'
import { MessageContext } from './state'

export default function MessageEditorPage() {
  const { i18n } = useTranslation()
  const { childId } = useNonNullableParams<{
    unitId: UUID
    groupId: UUID
    childId: UUID
  }>()

  const navigate = useNavigate()
  const { accounts, selectedAccount } = useContext(MessageContext)
  const [messageReceivers] = useApiState(getReceivers, [])
  const [sending, setSending] = useState(false)

  const receivers = useMemo(() => {
    const findChildReceivers = (receiver: MessageReceiver): MessageReceiver[] =>
      receiver.type === 'CHILD' && receiver.id === childId
        ? [receiver]
        : 'receivers' in receiver
        ? receiver.receivers.flatMap(findChildReceivers)
        : []

    return messageReceivers.map((accounts) =>
      accounts
        .map((account) => ({
          ...account,
          receivers: account.receivers.flatMap(findChildReceivers)
        }))
        .filter((account) => account.receivers.length > 0)
    )
  }, [childId, messageReceivers])

  const onSend = useCallback(
    (accountId: UUID, messageBody: PostMessageBody) => {
      setSending(true)
      void postMessage(accountId, messageBody).then((res) => {
        if (res.isSuccess) {
          navigate(-1)
        } else {
          // TODO handle eg. expired pin session correctly
          console.error('Failed to send message')
        }
        setSending(false)
      })
    },
    [navigate]
  )

  const onDiscard = useCallback(
    (accountId: UUID, draftId: UUID) => {
      void deleteDraft(accountId, draftId).then(() => navigate(-1))
    },
    [navigate]
  )

  const onHide = useCallback(() => {
    navigate(-1)
  }, [navigate])

  return renderResult(combine(accounts, receivers), ([accounts, receivers]) =>
    receivers.length > 0 && selectedAccount ? (
      <MessageEditor
        availableReceivers={receivers}
        defaultSender={{
          value: selectedAccount.account.id,
          label: selectedAccount.account.name
        }}
        deleteAttachment={deleteAttachment}
        draftContent={undefined}
        getAttachmentUrl={getAttachmentUrl}
        i18n={{
          ...i18n.messages.messageEditor,
          ...i18n.fileUpload,
          ...i18n.common
        }}
        initDraftRaw={initDraft}
        mobileVersion={true}
        accounts={accounts}
        onClose={onHide}
        onDiscard={onDiscard}
        onSend={onSend}
        saveDraftRaw={saveDraft}
        saveMessageAttachment={saveMessageAttachment}
        sending={sending}
      />
    ) : receivers.length === 0 ? (
      <ContentArea
        opaque
        paddingVertical="zero"
        paddingHorizontal="zero"
        fullHeight={true}
        data-qa="messages-editor-content-area"
      >
        <TopBar title={i18n.messages.newMessage} />
        <PaddedContainer>
          <span data-qa="info-no-receivers">{i18n.messages.noReceivers}</span>
        </PaddedContainer>
        <BackButtonInline
          onClick={() => navigate(-1)}
          icon={faArrowLeft}
          text={i18n.common.back}
        />
      </ContentArea>
    ) : null
  )
}

const PaddedContainer = styled.div`
  padding: ${defaultMargins.m} ${defaultMargins.s};
`
