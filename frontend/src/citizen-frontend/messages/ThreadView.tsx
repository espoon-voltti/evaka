// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faReply } from '@fortawesome/free-solid-svg-icons'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState
} from 'react'
import styled from 'styled-components'

import {
  Message,
  MessageThread
} from 'lib-common/generated/api-types/messaging'
import { formatPreferredName } from 'lib-common/names'
import { UUID } from 'lib-common/types'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import { StaticChip } from 'lib-components/atoms/Chip'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Linkify from 'lib-components/atoms/Linkify'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { desktopMin } from 'lib-components/breakpoints'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { MessageReplyEditor } from 'lib-components/molecules/MessageReplyEditor'
import { ThreadContainer } from 'lib-components/molecules/ThreadListItem'
import { fontWeights, H2, InformationText } from 'lib-components/typography'
import { useRecipients } from 'lib-components/utils/useReplyRecipients'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors, { theme } from 'lib-customizations/common'
import { faTrash } from 'lib-icons'

import { getAttachmentUrl } from '../attachments'
import { useTranslation } from '../localization'

import { MessageCharacteristics } from './MessageCharacteristics'
import { ConfirmDeleteThread, MessageContainer } from './MessageComponents'
import { MessageContext } from './state'

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
  background: ${colors.grayscale.g0};
  max-height: 215px; // fits roughly 5 rows of heading text with the chip and paddings
  overflow: auto;

  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-start;
  gap: ${defaultMargins.xs};
  padding: ${defaultMargins.s};

  @media screen and (min-width: ${desktopMin}) {
    flex-direction: row-reverse;
    gap: ${defaultMargins.s};
    padding: ${defaultMargins.L};
  }
`

const SenderName = styled.div`
  font-weight: ${fontWeights.semibold};
`

const MessageContent = styled.div`
  padding-top: ${defaultMargins.s};
  white-space: pre-line;
`

const ActionRow = styled(FixedSpaceRow)`
  margin: 0 28px 0 28px;
`

const ReplyToThreadButton = styled(InlineButton)`
  align-self: flex-start;
`

const SingleMessage = React.memo(function SingleMessage({
  message
}: {
  message: Message
}) {
  return (
    <MessageContainer role="comment">
      <TitleRow>
        <SenderName>{message.sender.name}</SenderName>
        <InformationText>
          <time dateTime={message.sentAt.formatIso()}>
            {message.sentAt.toLocalDate().format()}
          </time>
        </InformationText>
      </TitleRow>
      <InformationText>
        {(message.recipientNames
          ? message.recipientNames
          : message.recipients.map((r) => r.name)
        ).join(', ')}
      </InformationText>
      <MessageContent data-qa="thread-reader-content">
        <Linkify>{message.content}</Linkify>
      </MessageContent>
      {message.attachments.length > 0 && (
        <>
          <HorizontalLine slim />
          <FixedSpaceColumn spacing="xs">
            {message.attachments.map((attachment) => (
              <FileDownloadButton
                key={attachment.id}
                file={attachment}
                getFileUrl={getAttachmentUrl}
                icon
                data-qa="attachment"
              />
            ))}
          </FixedSpaceColumn>
        </>
      )}
    </MessageContainer>
  )
})

const AutoScrollPositionSpan = styled.span<{ top: string }>`
  position: absolute;
  top: ${(p) => p.top};
`

interface Props {
  accountId: UUID
  thread: MessageThread
  onThreadDeleted: () => void
}

export default React.memo(function ThreadView({
  accountId,
  thread: { id: threadId, messages, title, type, urgent, children },
  onThreadDeleted
}: Props) {
  const i18n = useTranslation()
  const { sendReply, replyState, setReplyContent, getReplyContent } =
    useContext(MessageContext)

  const { onToggleRecipient, recipients } = useRecipients(messages, accountId)
  const [replyEditorVisible, setReplyEditorVisible] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [stickyTitleRowHeight, setStickyTitleRowHeight] = useState(0)
  const stickyTitleRowRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    setStickyTitleRowHeight(stickyTitleRowRef.current?.clientHeight || 0)
  }, [setStickyTitleRowHeight, stickyTitleRowRef])

  useEffect(() => setReplyEditorVisible(false), [threadId])

  const autoScrollRef = useRef<HTMLSpanElement>(null)
  useEffect(() => {
    scrollRefIntoView(autoScrollRef)
  }, [messages, replyEditorVisible])

  const autoFocusRef = useRef<HTMLButtonElement>(null)
  useEffect(() => {
    autoFocusRef.current?.focus()
  }, [messages, replyEditorVisible])

  const onUpdateContent = useCallback(
    (content: string) => setReplyContent(threadId, content),
    [setReplyContent, threadId]
  )

  const onDiscard = useCallback(() => {
    setReplyContent(threadId, '')
    setReplyEditorVisible(false)
  }, [setReplyContent, setReplyEditorVisible, threadId])

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
      sending: `${i18n.messages.sending}...`,
      discard: i18n.messages.messageEditor.discard
    }),
    [i18n]
  )

  return (
    <ThreadContainer data-qa="thread-reader">
      <StickyTitleRow ref={stickyTitleRowRef}>
        <FixedSpaceFlexWrap>
          <MessageCharacteristics
            type={type}
            urgent={urgent}
            labels={i18n.messages.types}
          />
          {children.map((child) => (
            <StaticChip key={child.childId} color={theme.colors.main.m2}>
              {formatPreferredName(child) || ''}
            </StaticChip>
          ))}
        </FixedSpaceFlexWrap>
        <H2 noMargin data-qa="thread-reader-title">
          {title}
        </H2>
      </StickyTitleRow>
      <Gap size="s" />
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
      {replyEditorVisible ? (
        <MessageContainer>
          <MessageReplyEditor
            replyState={replyState}
            onSubmit={onSubmit}
            onUpdateContent={onUpdateContent}
            onDiscard={onDiscard}
            recipients={recipients}
            onToggleRecipient={onToggleRecipient}
            replyContent={replyContent}
            i18n={editorLabels}
          />
        </MessageContainer>
      ) : (
        messages.length > 0 && (
          <>
            <Gap size="s" />
            <ActionRow justifyContent="space-between">
              {type === 'MESSAGE' ? (
                <ReplyToThreadButton
                  icon={faReply}
                  onClick={() => setReplyEditorVisible(true)}
                  data-qa="message-reply-editor-btn"
                  text={i18n.messages.replyToThread}
                  ref={autoFocusRef}
                />
              ) : (
                <div />
              )}
              <InlineButton
                icon={faTrash}
                aria-label={i18n.common.delete}
                data-qa="delete-thread-btn"
                className="delete-btn"
                onClick={() => setConfirmDelete(true)}
                text={i18n.messages.deleteThread}
              />
            </ActionRow>
            <Gap size="m" />
          </>
        )
      )}
      {replyEditorVisible && <span ref={autoScrollRef} />}
      {confirmDelete && (
        <ConfirmDeleteThread
          threadId={threadId}
          onClose={() => setConfirmDelete(false)}
          onSuccess={() => {
            setConfirmDelete(false)
            onThreadDeleted()
          }}
        />
      )}
    </ThreadContainer>
  )
})
