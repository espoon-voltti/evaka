// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import { fontWeights, H2 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
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
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { getAttachmentBlob } from '../attachments'
import { OverlayContext } from '../overlay/state'

const TitleRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;

  & + & {
    margin-top: ${defaultMargins.L};
  }
`

const StickyTitleRow = styled(TitleRow)`
  position: sticky;
  top: 0;
  padding: 15px;
  background: white;
  max-height: 100px;
  overflow: auto;
`

const StickyTitleRowTitle = styled(H2)`
  top: 0;
  padding: 15px;
  background: white;
  max-height: 100px;
`

const SenderName = styled.div`
  font-weight: ${fontWeights.semibold};
`

const SentDate = styled.div`
  font-size: 14px;
  font-weight: ${fontWeights.semibold};
  color: ${colors.greyscale.dark};
`

const MessageContent = styled.div`
  padding-top: ${defaultMargins.s};
  white-space: pre-line;
`

const ReplyToThreadButton = styled(InlineButton)`
  padding-left: 28px;
`

function SingleMessage({ message }: { message: Message }) {
  const i18n = useTranslation()
  const { setErrorMessage } = useContext(OverlayContext)

  return (
    <MessageContainer>
      <TitleRow>
        <SenderName>{message.sender.name}</SenderName>
        <SentDate>{formatDate(message.sentAt)}</SentDate>
      </TitleRow>
      <span>
        {(message.recipientNames
          ? message.recipientNames
          : message.recipients.map((r) => r.name)
        ).join(', ')}
      </span>
      <MessageContent data-qa="thread-reader-content">
        {message.content}
      </MessageContent>
      {message.attachments.length > 0 && (
        <>
          <HorizontalLine slim />
          <FixedSpaceColumn spacing="xs">
            {message.attachments.map((attachment) => (
              <FileDownloadButton
                key={attachment.id}
                file={attachment}
                fileFetchFn={getAttachmentBlob}
                onFileUnavailable={() =>
                  setErrorMessage({
                    type: 'error',
                    title: i18n.fileDownload.modalHeader,
                    text: i18n.fileDownload.modalMessage
                  })
                }
                icon
                data-qa={'attachment'}
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

const AutoScrollPositionSpan = styled.span<{ top: string }>`
  position: absolute;
  top: ${(p) => p.top};
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
  const [stickyTitleRowHeight, setStickyTitleRowHeight] = useState<number>(0)
  const stickyTitleRowRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    setStickyTitleRowHeight(stickyTitleRowRef.current?.clientHeight || 0)
  }, [setStickyTitleRowHeight, stickyTitleRowRef])

  useEffect(() => setReplyEditorVisible(false), [threadId])

  const autoScrollRef = useRef<HTMLSpanElement>(null)
  const scrollToBottom = () => {
    autoScrollRef.current?.scrollIntoView({ behavior: 'smooth' })
  }
  useEffect(() => {
    scrollToBottom()
  }, [messages, replyEditorVisible])

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
      {title && type && (
        <StickyTitleRow ref={stickyTitleRowRef}>
          <StickyTitleRowTitle data-qa="thread-reader-title">
            {title}
          </StickyTitleRowTitle>
          <MessageTypeChip type={type} labels={i18n.messages.types} />
        </StickyTitleRow>
      )}
      {messages.map((message, idx) => (
        <React.Fragment key={`${message.id}-fragment`}>
          {idx === messages.length - 1 && !replyEditorVisible && (
            <div style={{ position: 'relative' }}>
              <AutoScrollPositionSpan
                top={`-${stickyTitleRowHeight}px`}
                ref={autoScrollRef}
              />
            </div>
          )}
          <SingleMessage key={message.id} message={message} />
        </React.Fragment>
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
            <Gap size={'m'} />
          </>
        ))}
      {replyEditorVisible && <span ref={autoScrollRef} />}
    </ThreadContainer>
  )
})
