// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState
} from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import styled, { css } from 'styled-components'

import { routes } from 'employee-mobile-frontend/App'
import { combine } from 'lib-common/api'
import {
  Message,
  MessageAccount,
  MessageChild,
  MessageThread,
  SentMessage
} from 'lib-common/generated/api-types/messaging'
import { formatAccountNames } from 'lib-common/messaging'
import { queryOrDefault, useChainedQuery } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Linkify from 'lib-components/atoms/Linkify'
import { Button } from 'lib-components/atoms/buttons/Button'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import MessageReplyEditor from 'lib-components/messages/MessageReplyEditor'
import { ThreadContainer } from 'lib-components/messages/ThreadListItem'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { fontWeights, InformationText } from 'lib-components/typography'
import { useRecipients } from 'lib-components/utils/useReplyRecipients'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faReply } from 'lib-icons'

import { renderResult } from '../async-rendering'
import TopBar from '../common/TopBar'
import { useTranslation } from '../common/i18n'
import { toUnitOrGroup, UnitOrGroup } from '../common/unit-or-group'

import { getAttachmentUrl } from './api'
import { replyToThreadMutation, threadQuery } from './queries'
import { MessageContext } from './state'

export const ReceivedThreadPage = React.memo(function ReceivedThreadPage({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const navigate = useNavigate()
  const { threadId } = useRouteParams(['threadId'])
  const { groupAccount } = useContext(MessageContext)

  const selectedAccount = groupAccount(
    unitOrGroup.type === 'group' ? unitOrGroup.id : null
  )
  const thread = useChainedQuery(
    selectedAccount.map((selectedAccount) =>
      queryOrDefault(
        threadQuery,
        null
      )(
        selectedAccount !== undefined
          ? { accountId: selectedAccount.account.id, threadId }
          : undefined
      )
    )
  )
  return renderResult(
    combine(selectedAccount, thread),
    ([selectedAccount, thread]) =>
      selectedAccount !== undefined && thread !== null ? (
        <ContentArea
          opaque={false}
          fullHeight
          paddingHorizontal="zero"
          paddingVertical="zero"
          data-qa="messages-page-content-area"
        >
          <ReceivedThread
            accountId={selectedAccount.account.id}
            thread={thread}
            onBack={() => navigate(routes.messages(unitOrGroup).value)}
            unitId={unitOrGroup.unitId}
          />
        </ContentArea>
      ) : (
        <Navigate
          to={routes.messages(toUnitOrGroup(unitOrGroup.unitId)).value}
        />
      )
  )
})

interface ReceivedThreadProps {
  unitId: UUID
  accountId: UUID
  thread: MessageThread
  onBack: () => void
}

const ReceivedThread = React.memo(function ReceivedThread({
  unitId,
  accountId,
  thread: { id: threadId, messages, title, type, children },
  onBack
}: ReceivedThreadProps) {
  const { i18n } = useTranslation()
  const { setReplyContent, getReplyContent } = useContext(MessageContext)

  const { onToggleRecipient, recipients } = useRecipients(messages, accountId)
  const [replyEditorVisible, setReplyEditorVisible] = useState(
    () => getReplyContent(threadId) !== ''
  )
  const showReplyEditor = useCallback(() => setReplyEditorVisible(true), [])
  const hideReplyEditor = useCallback(() => setReplyEditorVisible(false), [])

  const lastMessageRef = useRef<HTMLLIElement>(null)

  const onUpdateContent = useCallback(
    (content: string) => setReplyContent(threadId, content),
    [setReplyContent, threadId]
  )

  const onDiscard = useCallback(() => {
    setReplyContent(threadId, '')
    hideReplyEditor()
  }, [setReplyContent, hideReplyEditor, threadId])

  const replyContent = getReplyContent(threadId)
  const sendEnabled = !!replyContent && recipients.some((r) => r.selected)

  const endOfMessagesRef = useRef<HTMLDivElement | null>(null)
  useEffect(() => {
    scrollRefIntoView(lastMessageRef)
  }, [])

  return (
    <MobileThreadContainer data-qa="thread-reader">
      <TopBar title={title} onBack={onBack} invertedColors unitId={unitId} />
      <Gap size="s" />
      <MessageList>
        {messages.map((message, i) => (
          <React.Fragment key={message.id}>
            <SingleMessage
              message={message}
              relatedChildren={children}
              ref={i === messages.length - 1 ? lastMessageRef : undefined}
            />
            <Gap size="xs" />
          </React.Fragment>
        ))}
      </MessageList>
      <div ref={endOfMessagesRef} />
      {replyEditorVisible ? (
        <ReplyEditorContainer>
          <MessageReplyEditor
            mutation={replyToThreadMutation}
            onSubmit={() => ({
              accountId,
              threadId,
              messageId: messages.slice(-1)[0].id,
              body: {
                content: replyContent,
                recipientAccountIds: recipients
                  .filter((r) => r.selected)
                  .map((r) => r.id)
              }
            })}
            onUpdateContent={onUpdateContent}
            onDiscard={onDiscard}
            onSuccess={hideReplyEditor}
            recipients={recipients}
            onToggleRecipient={onToggleRecipient}
            replyContent={replyContent}
            sendEnabled={sendEnabled}
          />
        </ReplyEditorContainer>
      ) : (
        messages.length > 0 && (
          <>
            <Gap size="s" />
            <ActionRow justifyContent="space-between">
              {type === 'MESSAGE' ? (
                <ReplyToThreadButton
                  appearance="inline"
                  icon={faReply}
                  onClick={showReplyEditor}
                  data-qa="message-reply-editor-btn"
                  text={i18n.messages.thread.reply}
                />
              ) : (
                <div />
              )}
            </ActionRow>
            <Gap size="m" />
          </>
        )
      )}
    </MobileThreadContainer>
  )
})

const SingleMessage = React.memo(
  React.forwardRef(function SingleMessage(
    {
      message,
      relatedChildren
    }: {
      message: Message
      relatedChildren: MessageChild[]
    },
    ref: React.ForwardedRef<HTMLLIElement>
  ) {
    const { senderName, recipientNames } = useMemo(
      () =>
        formatAccountNames(message.sender, message.recipients, relatedChildren),
      [message.recipients, message.sender, relatedChildren]
    )
    return (
      <MessageContainer tabIndex={-1} ref={ref}>
        <TitleRow>
          <SenderName data-qa="single-message-sender-name">
            {senderName}
          </SenderName>
          <InformationText>
            {message.sentAt.toLocalDate().format()}
          </InformationText>
        </TitleRow>
        <InformationText>{recipientNames.join(', ')}</InformationText>
        <MessageContent data-qa="single-message-content">
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
  })
)

interface SentMessageViewProps {
  unitId: UUID
  account: MessageAccount
  message: SentMessage
  onBack: () => void
}

export const SentMessageView = React.memo(function SentMessageView({
  unitId,
  account,
  message,
  onBack
}: SentMessageViewProps) {
  return (
    <MobileThreadContainer data-qa="thread-reader">
      <TopBar
        title={message.threadTitle}
        onBack={onBack}
        invertedColors
        unitId={unitId}
      />
      <Gap size="s" />
      <MessageList>
        <MessageContainer>
          <TitleRow>
            <SenderName data-qa="single-message-sender-name">
              {account.name}
            </SenderName>
            <InformationText>
              {message.sentAt.toLocalDate().format()}
            </InformationText>
          </TitleRow>
          <InformationText>{message.recipientNames.join(', ')}</InformationText>
          <MessageContent data-qa="single-message-content">
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
      </MessageList>
    </MobileThreadContainer>
  )
})

const MobileThreadContainer = styled(ThreadContainer)`
  height: 100vh;
  overflow-y: auto;
`

const TitleRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;

  & + & {
    margin-top: ${defaultMargins.L};
  }
`

const MessageList = styled.ul`
  margin: 0;
  padding: 0;
  list-style: none;
`

const SenderName = styled.div`
  font-weight: ${fontWeights.semibold};
`

const MessageContent = styled.div`
  padding-top: ${defaultMargins.s};
  white-space: pre-line;
`

const ActionRow = styled(FixedSpaceRow)`
  padding: 0 ${defaultMargins.xs} ${defaultMargins.xs} ${defaultMargins.xs};
`

const ReplyToThreadButton = styled(Button)`
  align-self: flex-start;
`

const messageContainerStyles = css`
  background-color: ${colors.grayscale.g0};
  padding: ${defaultMargins.s};
`

const MessageContainer = styled.li`
  ${messageContainerStyles}
  h2 {
    margin: 0;
  }
`

const ReplyEditorContainer = styled.div`
  ${messageContainerStyles}
`
