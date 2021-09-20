// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import { H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'
import { MessageReplyEditor } from 'lib-components/molecules/MessageReplyEditor'
import { useRecipients } from 'lib-components/utils/useReplyRecipients'
import {
  Message,
  MessageThread
} from 'lib-common/generated/api-types/messaging'
import { useTranslation } from '../localization'
import { MessageContainer } from './MessageComponents'
import { MessageTypeChip } from './MessageTypeChip'
import { MessageContext } from './state'
import { formatDate } from 'lib-common/date'
import { faReply } from '@fortawesome/free-solid-svg-icons'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { MessageType } from 'lib-common/generated/enums'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import {FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import FileDownloadButton from "../../lib-components/molecules/FileDownloadButton";
import {getAttachmentBlob} from "../attachments";

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

const ReplyToThreadButton = styled(InlineButton)`
  padding-left: 28px;
`

function SingleMessage({
  title,
  type,
  message
}: {
  message: Message
  type?: MessageType
  title?: string
}) {
  const i18n = useTranslation()
  return (
    <MessageContainer>
      {title && type && (
        <TitleRow>
          <H2 data-qa="thread-reader-title">{title}</H2>
          <MessageTypeChip type={type} labels={i18n.messages.types} />
        </TitleRow>
      )}
      <TitleRow>
        <SenderName>{message.sender.name}</SenderName>
        <SentDate>{formatDate(message.sentAt)}</SentDate>
      </TitleRow>
      <span>{message.recipients.map((r) => r.name).join(', ')}</span>
      <MessageContent data-qa="thread-reader-content">
        {message.content}
      </MessageContent>
      {message.attachments.length > 0 && (
        <>
          <HorizontalLine slim/>
          <FixedSpaceColumn spacing='xs'>
            {message.attachments.map((attachment) => (
              <FileDownloadButton
                key={attachment.id}
                file={attachment}
                fileFetchFn={getAttachmentBlob}
                onFileUnavailable={() => console.error('oops')}
                icon
              />
            ))}
          </FixedSpaceColumn>
        </>
      )}
    </MessageContainer>
  )
}

const ThreadContainer = styled.div`
  width: 100%;
  box-sizing: border-box;
  min-width: 300px;
  max-width: 100%;
  min-height: 500px;
  overflow-y: auto;
`

interface Props {
  accountId: UUID
  thread: MessageThread
}

export default React.memo(function ThreadView({
  accountId,
  thread: { id: threadId, messages, title, type }
}: Props) {
  const i18n = useTranslation()
  const { sendReply, replyState, setReplyContent, getReplyContent } =
    useContext(MessageContext)

  const { onToggleRecipient, recipients } = useRecipients(messages, accountId)
  const [replyEditorVisible, setReplyEditorVisible] = useState<boolean>(false)

  useEffect(() => setReplyEditorVisible(false), [threadId])

  const onUpdateContent = useCallback(
    (content) => setReplyContent(threadId, content),
    [setReplyContent, threadId]
  )

  const replyContent = getReplyContent(threadId)
  const onSubmit = () =>
    sendReply({
      content: replyContent,
      messageId: messages.slice(-1)[0].id,
      recipientAccountIds: recipients
        .filter((r) => r.selected)
        .map((r) => r.id),
      staffAnnotation: i18n.messages.staffAnnotation
    })

  const editorLabels = useMemo(
    () => ({
      add: i18n.common.add,
      message: i18n.messages.types.MESSAGE,
      messagePlaceholder: i18n.messages.messagePlaceholder,
      recipients: i18n.messages.recipients,
      send: i18n.messages.send,
      sending: `${i18n.messages.sending}...`
    }),
    [i18n]
  )
  return (
    <ThreadContainer>
      {messages.map((message, idx) => (
        <SingleMessage
          key={message.id}
          message={message}
          title={idx === 0 ? title : undefined}
          type={idx === 0 ? type : undefined}
        />
      ))}
      {type === 'MESSAGE' &&
        messages.length > 0 &&
        (replyEditorVisible ? (
          <MessageContainer>
            <MessageReplyEditor
              replyState={replyState}
              onSubmit={onSubmit}
              onUpdateContent={onUpdateContent}
              recipients={recipients}
              onToggleRecipient={onToggleRecipient}
              replyContent={replyContent}
              i18n={editorLabels}
            />
          </MessageContainer>
        ) : (
          <>
            <Gap size={'s'} />
            <ReplyToThreadButton
              icon={faReply}
              onClick={() => setReplyEditorVisible(true)}
              data-qa="message-reply-editor-btn"
              text={i18n.messages.replyToThread}
            />
          </>
        ))}
    </ThreadContainer>
  )
})
