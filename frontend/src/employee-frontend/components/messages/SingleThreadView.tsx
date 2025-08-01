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
import { Link, useLocation, useSearchParams } from 'wouter'

import { wrapResult } from 'lib-common/api'
import type {
  Message,
  MessageChild,
  MessageThread,
  MessageType,
  ThreadReply
} from 'lib-common/generated/api-types/messaging'
import type { MessageAccountId } from 'lib-common/generated/api-types/shared'
import { formatAccountNames } from 'lib-common/messaging'
import { useMutationResult } from 'lib-common/query'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Linkify from 'lib-components/atoms/Linkify'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import LegacyInlineButton from 'lib-components/atoms/buttons/LegacyInlineButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { MessageCharacteristics } from 'lib-components/messages/MessageCharacteristics'
import MessageReplyEditor from 'lib-components/messages/MessageReplyEditor'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { Bold, H2, InformationText } from 'lib-components/typography'
import { useRecipients } from 'lib-components/utils/useReplyRecipients'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faAngleLeft, faBoxArchive, faEnvelope } from 'lib-icons'

import { getAttachmentUrl } from '../../api/attachments'
import { archiveThread } from '../../generated/api-clients/messaging'
import { useTranslation } from '../../state/i18n'

import { MessageContext } from './MessageContext'
import {
  markLastReceivedMessageInThreadUnreadMutation,
  markThreadReadMutation,
  replyToThreadMutation
} from './queries'
import type { View } from './types-view'
import { isFolderView, isStandardView } from './types-view'

const archiveThreadResult = wrapResult(archiveThread)

const MessageContainer = styled.div`
  background-color: ${colors.grayscale.g0};
  padding: ${defaultMargins.L};
  margin-top: ${defaultMargins.s};

  h2 {
    margin: 0;
  }
`

const TitleRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: ${defaultMargins.xxs};

  & + & {
    margin-top: ${defaultMargins.L};
  }
`

const ThreadHeader = styled.div`
  position: sticky;
  top: 0;
  padding: ${defaultMargins.L};
  background: ${colors.grayscale.g0};
  max-height: 150px;
`

const MessageContent = styled.div`
  padding-top: ${defaultMargins.s};
  white-space: pre-line;
  word-break: break-word;
`

function SingleMessage({
  view,
  message,
  messageChildren,
  index
}: {
  view: View
  message: Message
  messageChildren: MessageChild[]
  type?: MessageType
  title?: string
  index: number
}) {
  const { senderName, recipientNames } = useMemo(() => {
    if (view === 'sent' || view === 'copies') {
      return {
        senderName: message.sender.name,
        // message.recipientNames should always exist for sent messages, ?? is there to satisfy the type checker
        recipientNames: message.recipientNames ?? []
      }
    } else {
      return formatAccountNames(
        message.sender,
        message.recipients,
        messageChildren
      )
    }
  }, [
    view,
    message.sender,
    message.recipients,
    message.recipientNames,
    messageChildren
  ])
  return (
    <MessageContainer>
      <TitleRow>
        <Bold>{senderName}</Bold>
        <InformationText>{message.sentAt.format()}</InformationText>
      </TitleRow>
      <InformationText data-qa="recipient-names">
        {recipientNames.join(', ')}
      </InformationText>
      <MessageContent data-qa="message-content" data-index={index}>
        <Linkify text={message.content} />
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
}

const ThreadContainer = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
`
const ScrollContainer = styled.div`
  overflow-y: auto;
`

const AutoScrollPositionSpan = styled.span<{ top: string }>`
  position: absolute;
  top: ${(p) => p.top};
`

interface Props {
  accountId: MessageAccountId
  goBack: () => void
  thread: MessageThread
  view: View
  onArchived?: () => void
}

export function SingleThreadView({
  accountId,
  goBack,
  thread: {
    id: threadId,
    messages,
    title,
    type,
    urgent,
    sensitive,
    children,
    applicationId,
    applicationType
  },
  view,
  onArchived
}: Props) {
  const { i18n } = useTranslation()
  const [, navigate] = useLocation()
  const { getReplyContent, onReplySent, setReplyContent, refreshMessages } =
    useContext(MessageContext)
  const [searchParams] = useSearchParams()
  const [replyEditorVisible, setReplyEditorVisible] = useState<boolean>(
    !!searchParams.get('reply')
  )
  const [stickyTitleRowHeight, setStickyTitleRowHeight] = useState<number>(0)
  const stickyTitleRowRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    setStickyTitleRowHeight(stickyTitleRowRef.current?.clientHeight || 0)
  }, [setStickyTitleRowHeight, stickyTitleRowRef])

  const replyContent = getReplyContent(threadId)
  const onUpdateContent = useCallback(
    (content: string) => setReplyContent(threadId, content),
    [setReplyContent, threadId]
  )
  const { recipients, onToggleRecipient } = useRecipients(
    messages,
    accountId,
    null
  )

  const autoScrollRef = useRef<HTMLSpanElement>(null)
  useEffect(() => {
    scrollRefIntoView(autoScrollRef)
  }, [messages, replyEditorVisible])

  const onSubmitReply = useCallback(
    () => ({
      accountId,
      messageId: messages.slice(-1)[0].id,
      body: {
        content: replyContent,
        recipientAccountIds: recipients
          .filter((r) => r.selected)
          .map((r) => r.id)
      }
    }),
    [accountId, messages, recipients, replyContent]
  )

  const handleReplySent = useCallback(
    (response: ThreadReply) => {
      onReplySent(response)
      setReplyEditorVisible(false)
    },
    [onReplySent]
  )

  const onDiscard = useCallback(() => {
    setReplyContent(threadId, '')
    setReplyEditorVisible(false)
  }, [setReplyContent, setReplyEditorVisible, threadId])

  const canReply = type === 'MESSAGE'
  const sendEnabled = !!replyContent && recipients.some((r) => r.selected)

  const { mutateAsync: markThreadRead } = useMutationResult(
    markThreadReadMutation
  )
  useEffect(() => {
    const hasUnreadMessages = messages.some(
      (m) => !m.readAt && m.sender.id !== accountId
    )
    if (hasUnreadMessages) {
      void markThreadRead({ accountId, threadId }).then(() => {
        refreshMessages(accountId)
      })
    }
  }, [accountId, markThreadRead, messages, refreshMessages, threadId])

  const singleCustomer = useMemo(() => {
    if (messages.length === 0) return null
    const firstMessage = messages[0]
    if (
      firstMessage.sender.type !== 'FINANCE' &&
      firstMessage.sender.type !== 'SERVICE_WORKER'
    )
      return null
    if (
      firstMessage.recipients.length !== 1 ||
      firstMessage.recipients[0].personId === null
    )
      return null
    return firstMessage.recipients[0]
  }, [messages])

  const isServiceWorkerApplicationThread = useMemo(() => {
    if (!applicationId) return false
    const firstMessage = messages[0]
    return firstMessage.sender.type === 'SERVICE_WORKER'
  }, [applicationId, messages])

  const hasCitizenMessages = useMemo(() => {
    return messages.some((message) => message.sender.type === 'CITIZEN')
  }, [messages])

  return (
    <ThreadContainer>
      <ContentArea opaque paddingVertical={defaultMargins.xs}>
        <LegacyInlineButton
          icon={faAngleLeft}
          text={i18n.common.goBack}
          onClick={goBack}
          color={colors.main.m2}
        />
      </ContentArea>
      <Gap size="xs" />
      <ScrollContainer>
        <div>
          <ThreadHeader ref={stickyTitleRowRef}>
            <FixedSpaceColumn>
              <FixedSpaceRow justifyContent="space-between">
                <H2 noMargin>
                  {title}
                  {sensitive && ` (${i18n.messages.sensitive})`}
                </H2>
                <MessageCharacteristics type={type} urgent={urgent} />
              </FixedSpaceRow>
              <FixedSpaceRow justifyContent="left">
                {singleCustomer && singleCustomer.personId && (
                  <div>
                    {i18n.messages.customer}:{' '}
                    <Link to={`/profile/${singleCustomer.personId}`}>
                      {singleCustomer.name}
                    </Link>
                  </div>
                )}
                {isServiceWorkerApplicationThread && (
                  <div>
                    {applicationType
                      ? i18n.messages.applicationTypes[applicationType]
                      : i18n.messages.application}
                    {': '}
                    <Link to={`/applications/${applicationId}`}>
                      {i18n.messages.showApplication}
                    </Link>
                  </div>
                )}
              </FixedSpaceRow>
            </FixedSpaceColumn>
          </ThreadHeader>
          {messages.map((message, idx) => (
            <React.Fragment key={`${message.id}-fragment`}>
              {!replyEditorVisible && idx === messages.length - 1 && (
                <div style={{ position: 'relative' }}>
                  <AutoScrollPositionSpan
                    top={`-${stickyTitleRowHeight}px`}
                    ref={autoScrollRef}
                  />
                </div>
              )}
              <SingleMessage
                key={message.id}
                view={view}
                message={message}
                messageChildren={children}
                index={idx}
              />
            </React.Fragment>
          ))}
        </div>
        {canReply &&
          (isFolderView(view) ||
            (isStandardView(view) && ['received', 'thread'].includes(view))) &&
          (replyEditorVisible ? (
            <MessageContainer>
              <MessageReplyEditor
                mutation={replyToThreadMutation}
                onSubmit={onSubmitReply}
                onSuccess={handleReplySent}
                onDiscard={onDiscard}
                onUpdateContent={onUpdateContent}
                recipients={recipients}
                onToggleRecipient={onToggleRecipient}
                replyContent={replyContent}
                sendEnabled={sendEnabled}
              />
            </MessageContainer>
          ) : (
            <>
              <Gap size="s" />
              <ActionRow justifyContent="space-between">
                <Button
                  appearance="inline"
                  icon={faReply}
                  onClick={() => setReplyEditorVisible(true)}
                  data-qa="message-reply-editor-btn"
                  text={i18n.messages.replyToThread}
                />
                {hasCitizenMessages && (
                  <MutateButton
                    appearance="inline"
                    icon={faEnvelope}
                    text={i18n.messages.markUnread}
                    data-qa="mark-unread-btn"
                    mutation={markLastReceivedMessageInThreadUnreadMutation}
                    onClick={() => ({ accountId, threadId })}
                    onSuccess={() => {
                      navigate('/messages')
                    }}
                  />
                )}
                {onArchived && (
                  <AsyncButton
                    appearance="inline"
                    icon={faBoxArchive}
                    aria-label={i18n.common.archive}
                    data-qa="delete-thread-btn"
                    className="delete-btn"
                    onClick={() => archiveThreadResult({ accountId, threadId })}
                    onSuccess={onArchived}
                    text={i18n.messages.archiveThread}
                    stopPropagation
                  />
                )}
              </ActionRow>
              <Gap size="m" />
            </>
          ))}
        {replyEditorVisible && <span ref={autoScrollRef} />}
      </ScrollContainer>
    </ThreadContainer>
  )
}

const ActionRow = styled(FixedSpaceRow)`
  padding: 0 28px 0 28px;
`
