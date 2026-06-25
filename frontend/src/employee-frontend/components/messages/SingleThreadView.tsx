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

import type {
  DeletedMessageContent,
  Message,
  MessageChild,
  MessageThread,
  ThreadReply
} from 'lib-common/generated/api-types/messaging'
import type { MessageContentId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalTime from 'lib-common/local-time'
import type { TypedMessageAccount } from 'lib-common/messaging'
import { formatAccountNames } from 'lib-common/messaging'
import {
  constantQuery,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
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
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { Bold, H2, InformationText } from 'lib-components/typography'
import { useRecipients } from 'lib-components/utils/useReplyRecipients'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faAngleLeft, faBoxArchive, faEnvelope, faTrash } from 'lib-icons'

import { getAttachmentUrl } from '../../api/attachments'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'

import { ConfirmDeleteMessage } from './ConfirmDeleteMessage'
import { DeletedMessageView } from './DeletedMessageView'
import { MessageContext } from './MessageContext'
import {
  archiveThreadMutation,
  deletedMessageContentQuery,
  markLastReceivedMessageInThreadUnreadMutation,
  markThreadReadMutation,
  replyToThreadMutation
} from './queries'
import type { View } from './types-view'
import { isFolderView, isStandardView } from './types-view'

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

const deletionWindowDays = 8

function SingleMessage({
  account,
  view,
  message,
  messageChildren,
  index,
  isJustDeleted,
  revealed,
  onSetRevealed,
  supportEmail,
  onDelete
}: {
  account: TypedMessageAccount
  view: View
  message: Message
  messageChildren: MessageChild[]
  index: number
  isJustDeleted: boolean
  revealed: boolean
  onSetRevealed: (revealed: boolean) => void
  supportEmail: string | null
  onDelete: (contentId: MessageContentId) => void
}) {
  const { i18n } = useTranslation()
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

  const isOwnMessage = message.sender.id === account.id
  const isDeleted = message.contentDeletedAt !== null
  const canDelete =
    account.type !== 'MUNICIPAL' &&
    isOwnMessage &&
    !isDeleted &&
    HelsinkiDateTime.now().isBefore(
      message.sentAt
        .toLocalDate()
        .addDays(deletionWindowDays)
        .toHelsinkiDateTime(LocalTime.MIN)
    )

  return (
    <MessageContainer>
      {isJustDeleted && (
        <>
          <AlertBox
            data-qa="message-deleted-banner"
            noMargin
            wide
            message={i18n.messages.deletion.afterDeletion.banner(supportEmail)}
          />
          <Gap $size="m" />
        </>
      )}
      <TitleRow>
        <Bold>{senderName}</Bold>
        <InformationText>{message.sentAt.format()}</InformationText>
      </TitleRow>
      <FixedSpaceRow $justifyContent="space-between" $alignItems="center">
        <InformationText data-qa="recipient-names">
          {recipientNames.join(', ')}
        </InformationText>
        {canDelete && (
          <Button
            appearance="inline"
            icon={faTrash}
            text={i18n.messages.deletion.deleteButton}
            onClick={() => onDelete(message.contentId)}
            data-qa="delete-message-btn"
          />
        )}
      </FixedSpaceRow>
      <MessageContent data-qa="message-content" data-index={index}>
        {isDeleted && isOwnMessage ? (
          <DeletedMessageView
            accountId={account.id}
            contentId={message.contentId}
            isJustDeleted={isJustDeleted}
            revealed={revealed}
            onSetRevealed={onSetRevealed}
          />
        ) : (
          <Linkify text={message.content} />
        )}
      </MessageContent>
      {!isDeleted && message.attachments.length > 0 && (
        <>
          <HorizontalLine $slim />
          <FixedSpaceColumn $spacing="xs">
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

const AutoScrollPositionSpan = styled.span<{ $top: string }>`
  position: absolute;
  top: ${(p) => p.$top};
`

interface Props {
  account: TypedMessageAccount
  goBack: () => void
  thread: MessageThread
  view: View
  onArchived?: () => void
}

export function SingleThreadView({
  account,
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
  const { getReplyContent, onReplySent, setReplyContent } =
    useContext(MessageContext)
  const { featureConfig } = useContext(UserContext)
  const [deleteModalContentId, setDeleteModalContentId] =
    useState<MessageContentId | null>(null)
  const [justDeletedContentIds, setJustDeletedContentIds] = useState<
    Set<MessageContentId>
  >(() => new Set())
  const [revealedContentIds, setRevealedContentIds] = useState<
    Set<MessageContentId>
  >(() => new Set())
  const onSetRevealed = useCallback(
    (contentId: MessageContentId, revealed: boolean) =>
      setRevealedContentIds((prev) => {
        const next = new Set(prev)
        if (revealed) {
          next.add(contentId)
        } else {
          next.delete(contentId)
        }
        return next
      }),
    []
  )
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
    account,
    null
  )

  const autoScrollRef = useRef<HTMLSpanElement>(null)
  useEffect(() => {
    scrollRefIntoView(autoScrollRef)
  }, [messages, replyEditorVisible])

  const onSubmitReply = useCallback(
    () => ({
      accountId: account.id,
      threadId,
      body: {
        content: replyContent,
        recipientAccountIds: recipients
          .filter((r) => r.selected)
          .map((r) => r.id)
      }
    }),
    [account.id, threadId, recipients, replyContent]
  )

  const handleReplySent = useCallback(
    (response: ThreadReply) => {
      onReplySent(response, account.id)
      setReplyEditorVisible(false)
    },
    [account.id, onReplySent]
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
  const { mutateAsync: archiveThread } = useMutationResult(
    archiveThreadMutation
  )
  useEffect(() => {
    const hasUnreadMessages = messages.some(
      (m) => !m.readAt && m.sender.id !== account.id
    )
    if (hasUnreadMessages) {
      void markThreadRead({ accountId: account.id, threadId })
    }
  }, [account.id, markThreadRead, messages, threadId])

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

  const hasCitizenMessages = useMemo(
    () => messages.some((message) => message.sender.type === 'CITIZEN'),
    [messages]
  )

  const supportEmail = featureConfig?.messageSupportEmail ?? null

  const firstMessage = messages.length > 0 ? messages[0] : undefined
  const ownDeletedFirstMessage = useMemo(
    () =>
      firstMessage?.contentDeletedAt != null &&
      firstMessage.sender.id === account.id
        ? {
            contentId: firstMessage.contentId,
            contentDeletedAt: firstMessage.contentDeletedAt
          }
        : undefined,
    [firstMessage, account.id]
  )

  const revealedFirstContent = useQueryResult(
    ownDeletedFirstMessage !== undefined &&
      revealedContentIds.has(ownDeletedFirstMessage.contentId)
      ? deletedMessageContentQuery({
          accountId: account.id,
          contentId: ownDeletedFirstMessage.contentId
        })
      : constantQuery<DeletedMessageContent | null>(null)
  )

  const isThreadContext = view !== 'sent' && view !== 'copies'
  const threadTitlePrefix =
    i18n.messages.deletion.afterDeletion.threadTitlePrefix
  const headerTitle = useMemo(() => {
    const revealedTitle = revealedFirstContent.getOrElse(null)?.title ?? null
    if (revealedTitle !== null) {
      return `${threadTitlePrefix} [${revealedTitle}]`
    }
    if (isThreadContext && ownDeletedFirstMessage !== undefined) {
      return `${threadTitlePrefix} ${ownDeletedFirstMessage.contentDeletedAt.toLocalDate().format()}`
    }
    return title
  }, [
    isThreadContext,
    ownDeletedFirstMessage,
    revealedFirstContent,
    threadTitlePrefix,
    title
  ])

  return (
    <ThreadContainer>
      <ContentArea $opaque $paddingVertical={defaultMargins.xs}>
        <LegacyInlineButton
          icon={faAngleLeft}
          text={i18n.common.goBack}
          onClick={goBack}
          color={colors.main.m2}
        />
      </ContentArea>
      <Gap $size="xs" />
      <ScrollContainer>
        <div>
          <ThreadHeader ref={stickyTitleRowRef}>
            <FixedSpaceColumn>
              <FixedSpaceRow $justifyContent="space-between">
                <H2 $noMargin data-qa="thread-title">
                  {headerTitle}
                  {sensitive && ` (${i18n.messages.sensitive})`}
                </H2>
                <MessageCharacteristics type={type} urgent={urgent} />
              </FixedSpaceRow>
              <FixedSpaceRow $justifyContent="left">
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
                    $top={`-${stickyTitleRowHeight}px`}
                    ref={autoScrollRef}
                  />
                </div>
              )}
              <SingleMessage
                key={message.id}
                account={account}
                view={view}
                message={message}
                messageChildren={children}
                index={idx}
                isJustDeleted={justDeletedContentIds.has(message.contentId)}
                revealed={revealedContentIds.has(message.contentId)}
                onSetRevealed={(revealed) =>
                  onSetRevealed(message.contentId, revealed)
                }
                supportEmail={supportEmail}
                onDelete={(contentId) => setDeleteModalContentId(contentId)}
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
              <Gap $size="s" />
              <ActionRow $justifyContent="space-between">
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
                    onClick={() => ({ accountId: account.id, threadId })}
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
                    onClick={() =>
                      archiveThread({ accountId: account.id, threadId })
                    }
                    onSuccess={onArchived}
                    text={i18n.messages.archiveThread}
                    stopPropagation
                  />
                )}
              </ActionRow>
              <Gap $size="m" />
            </>
          ))}
        {replyEditorVisible && <span ref={autoScrollRef} />}
      </ScrollContainer>
      {deleteModalContentId !== null && (
        <ConfirmDeleteMessage
          accountId={account.id}
          contentId={deleteModalContentId}
          onClose={() => setDeleteModalContentId(null)}
          onDeleted={() =>
            setJustDeletedContentIds((prev) => {
              const next = new Set(prev)
              next.add(deleteModalContentId)
              return next
            })
          }
        />
      )}
    </ThreadContainer>
  )
}

const ActionRow = styled(FixedSpaceRow)`
  padding: 0 28px 0 28px;
`
