// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import colors from 'lib-customizations/common'
import { defaultMargins } from 'lib-components/white-space'
import { fontWeights } from 'lib-components/typography'
import {
  Message,
  MessageThread,
  NestedMessageAccount
} from 'lib-common/generated/api-types/messaging'
import { formatTime } from 'lib-common/date'
import React, { useCallback, useContext, useEffect, useRef } from 'react'
import { MessageContext } from '../../state/messages'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faArrowLeft, faArrowRight } from 'lib-icons'
import { ThreadContainer } from 'lib-components/molecules/ThreadListItem'
import { useTranslation } from '../../state/i18n'
import { useRecipients } from 'lib-components/utils/useReplyRecipients'
import { UUID } from 'lib-common/types'
import TextArea from 'lib-components/atoms/form/TextArea'

interface ThreadViewProps {
  thread: MessageThread
  onBack: () => void
}

export const ThreadView = React.memo(function ThreadView({
  thread: { id: threadId, messages, title },
  onBack
}: ThreadViewProps) {
  const { i18n } = useTranslation()

  const {
    getReplyContent,
    sendReply,
    selectedSender,
    setReplyContent,
    nestedAccounts
  } = useContext(MessageContext)

  const onUpdateContent = useCallback(
    (content) => setReplyContent(threadId, content),
    [setReplyContent, threadId]
  )

  const replyContent = getReplyContent(threadId)

  const allRecipients = messages.flatMap((m) => m.recipients)

  const possibleSenderAccounts: NestedMessageAccount[] =
    nestedAccounts.isSuccess
      ? nestedAccounts.value.filter(({ account }) =>
          allRecipients.some((r) => r.id === account.id)
        )
      : []

  const senderAccountId: UUID | undefined =
    possibleSenderAccounts[0]?.account.id

  const { recipients } = useRecipients(messages, senderAccountId)

  const onSubmitReply = useCallback(() => {
    replyContent.length > 0 &&
      selectedSender?.id &&
      sendReply({
        content: replyContent,
        messageId: messages.slice(-1)[0].id,
        recipientAccountIds: recipients.map((r) => r.id),
        accountId: senderAccountId
      })
  }, [
    replyContent,
    selectedSender,
    sendReply,
    recipients,
    messages,
    senderAccountId
  ])

  const endOfMessagesRef = useRef<null | HTMLDivElement>(null)
  useEffect(() => {
    endOfMessagesRef.current?.scrollIntoView(true)
  }, [messages])

  return (
    <ThreadViewMobile data-qa={'thread-view-mobile'}>
      <ThreadViewTopbar onClick={onBack}>
        <FontAwesomeIcon
          icon={faArrowLeft}
          color={colors.blues.dark}
          height={defaultMargins.s}
        />
        <ThreadViewTitle>{title}</ThreadViewTitle>
      </ThreadViewTopbar>
      {messages.map((message) => (
        <SingleMessage
          key={message.id}
          message={message}
          ours={possibleSenderAccounts.some(
            ({ account }) => account.id === message.sender.id
          )}
        />
      ))}
      <div ref={endOfMessagesRef} />
      <ThreadViewReplyContainer>
        <ThreadViewReply>
          <TextArea
            value={replyContent}
            onChange={onUpdateContent}
            className={'thread-view-input'}
            wrapperClassName={'thread-view-input-wrapper'}
            placeholder={i18n.messages.inputPlaceholder}
            data-qa={'thread-reply-input'}
          />
          <RoundIconButton
            onClick={onSubmitReply}
            disabled={replyContent.length === 0}
            data-qa={'thread-reply-button'}
          >
            <FontAwesomeIcon icon={faArrowRight} />
          </RoundIconButton>
        </ThreadViewReply>
      </ThreadViewReplyContainer>
    </ThreadViewMobile>
  )
})

function SingleMessage({ message, ours }: { message: Message; ours: boolean }) {
  return (
    <MessageContainer ours={ours} data-qa={'single-message'}>
      <TitleRow>
        <SenderName data-qa={'single-message-sender-name'}>
          {message.sender.name}
        </SenderName>
        <SentDate white={ours}>{formatTime(message.sentAt)}</SentDate>
      </TitleRow>
      <MessageContent data-qa="single-message-content">
        {message.content}
      </MessageContent>
    </MessageContainer>
  )
}

const RoundIconButton = styled.button`
  width: ${defaultMargins.L};
  height: ${defaultMargins.L};
  min-width: ${defaultMargins.L};
  min-height: ${defaultMargins.L};
  max-width: ${defaultMargins.L};
  max-height: ${defaultMargins.L};
  background: ${colors.blues.primary};
  border: none;
  border-radius: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  &:disabled {
    background: ${colors.greyscale.lighter};
  }
`

const MessageContainer = styled.div`
  border-radius: ${defaultMargins.s};
  ${(p: { ours: boolean }) =>
    p.ours
      ? `
      border-bottom-right-radius: 2px;
      align-self: flex-end;
      background-color: ${colors.blues.primary};
      color: white;
    `
      : `
      border-bottom-left-radius: 2px;
      align-self: flex-start;
      background-color: ${colors.blues.lighter};
    `}
  padding: ${defaultMargins.s};
  margin: ${defaultMargins.xs};
`

const SentDate = styled.div`
  font-size: 14px;
  font-weight: ${fontWeights.semibold};
  color: ${(p: { white: boolean }) =>
    p.white ? colors.greyscale.white : colors.greyscale.dark};
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
  font-weight: ${fontWeights.semibold};
  margin-right: ${defaultMargins.m};
`

const MessageContent = styled.div`
  padding-top: ${defaultMargins.s};
  white-space: pre-line;
`

const ThreadViewMobile = styled(ThreadContainer)`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  overflow-y: unset;
  min-height: 100vh;
`

const ThreadViewTopbar = styled.div`
  position: sticky;
  top: 0;
  background: ${colors.greyscale.lightest};
  padding: ${defaultMargins.m};
  align-self: stretch;
  cursor: pointer;
`

const ThreadViewTitle = styled.span`
  margin-left: ${defaultMargins.s};
  color: ${colors.blues.dark};
  font-weight: ${fontWeights.semibold};
`

const ThreadViewReplyContainer = styled.div`
  position: sticky;
  bottom: 0;
  width: 100%;
  display: flex;
  align-items: center;
  background: ${colors.greyscale.white};
  margin-top: auto;
  padding: ${defaultMargins.xxs} ${defaultMargins.xs} ${defaultMargins.xs};
`

const ThreadViewReply = styled.div`
  display: flex;
  align-items: center;
  flex-grow: 1;
  gap: ${defaultMargins.xs};
  .thread-view-input-wrapper {
    display: block;
    width: 100%;
  }
  .thread-view-input {
    background: ${colors.greyscale.lightest};
    border-radius: ${defaultMargins.m};
  }
  .thread-view-input:not(:focus) {
    border-bottom-color: transparent;
  }
`
