// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Message,
  MessageThread,
  MessageType
} from 'lib-common/api-types/messaging/message'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { ContentArea } from 'lib-components/layout/Container'
import { MessageReplyEditor } from 'lib-components/molecules/MessageReplyEditor'
import { H2 } from 'lib-components/typography'
import { useRecipients } from 'lib-components/utils/useReplyRecipients'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faAngleLeft } from 'lib-icons'
import React, { useCallback, useContext, useMemo } from 'react'
import styled from 'styled-components'
import { DATE_FORMAT_DATE_TIME } from '../../constants'
import { useTranslation } from '../../state/i18n'
import { UUID } from '../../types'
import { formatDate } from '../../utils/date'
import { MessageContext } from './MessageContext'
import { MessageTypeChip } from './MessageTypeChip'

const MessageContainer = styled.div`
  background-color: white;
  padding: ${defaultMargins.L};

  & + & {
    margin-top: ${defaultMargins.s};
  }

  h2 {
    margin: 0;
  }
`

const TitleRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;

  & + & {
    margin-top: ${defaultMargins.L};
  }
`
const SenderName = styled.div`
  font-weight: 600;
`
const SentDate = styled.div`
  font-size: 14px;
  font-weight: 600;
  color: ${colors.greyscale.dark};
`
const MessageContent = styled.div`
  padding-top: ${defaultMargins.s};
  white-space: pre-line;
`

function Message({
  title,
  type,
  message
}: {
  message: Message
  type?: MessageType
  title?: string
}) {
  return (
    <MessageContainer>
      {title && type && (
        <TitleRow>
          <H2>{title}</H2> <MessageTypeChip type={type} />
        </TitleRow>
      )}
      <TitleRow>
        <SenderName>{message.senderName}</SenderName>
        <SentDate>{formatDate(message.sentAt, DATE_FORMAT_DATE_TIME)}</SentDate>
      </TitleRow>
      <span>{message.recipients.map((r) => r.name).join(', ')}</span>
      <MessageContent>{message.content}</MessageContent>
    </MessageContainer>
  )
}

const ThreadContainer = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
`
const ScrollContainer = styled.div`
  overflow-y: auto;
`

interface Props {
  accountId: UUID
  goBack: () => void
  thread: MessageThread
}

export function SingleThreadView({
  accountId,
  goBack,
  thread: { id: threadId, messages, title, type }
}: Props) {
  const { i18n } = useTranslation()
  const {
    getReplyContent,
    sendReply,
    replyState,
    setReplyContent
  } = useContext(MessageContext)

  const replyContent = getReplyContent(threadId)
  const onUpdateContent = useCallback(
    (content) => setReplyContent(threadId, content),
    [setReplyContent, threadId]
  )
  const { recipients, onToggleRecipient } = useRecipients(messages, accountId)

  const onSubmitReply = () =>
    sendReply({
      content: replyContent,
      messageId: messages.slice(-1)[0].id,
      recipientAccountIds: recipients
        .filter((r) => r.selected)
        .map((r) => r.id),
      accountId
    })

  const canReply = type === 'MESSAGE' || messages[0].senderId === accountId
  const editorLabels = useMemo(
    () => ({
      add: i18n.common.add,
      message: i18n.messages.messageEditor.message,
      recipients: i18n.messages.messageEditor.receivers,
      send: i18n.messages.messageEditor.send,
      sending: i18n.messages.messageEditor.sending
    }),
    [i18n]
  )
  return (
    <ThreadContainer>
      <ContentArea opaque>
        <InlineButton
          icon={faAngleLeft}
          text={i18n.common.goBack}
          onClick={goBack}
          color={colors.blues.primary}
        />
      </ContentArea>
      <Gap size="xs" />
      <ScrollContainer>
        {messages.map((message, idx) => (
          <Message
            key={message.id}
            message={message}
            title={idx === 0 ? title : undefined}
            type={idx === 0 ? type : undefined}
          />
        ))}
        {canReply && (
          <MessageContainer>
            <MessageReplyEditor
              recipients={recipients}
              onToggleRecipient={onToggleRecipient}
              replyContent={replyContent}
              onUpdateContent={onUpdateContent}
              i18n={editorLabels}
              onSubmit={onSubmitReply}
              replyState={replyState}
            />
          </MessageContainer>
        )}
      </ScrollContainer>
    </ThreadContainer>
  )
}
