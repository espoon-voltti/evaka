// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import colors from 'lib-customizations/common'
import { defaultMargins } from 'lib-components/white-space'
import { fontWeights } from 'lib-components/typography'
import {
  Message,
  MessageThread
} from 'lib-common/generated/api-types/messaging'
import { formatTime } from 'lib-common/date'
import React, { useCallback, useContext } from 'react'
import { MessageContext } from '../../state/messages'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faArrowLeft, faArrowRight } from 'lib-icons'
import InputField from 'lib-components/atoms/form/InputField'
import { ThreadContainer } from 'lib-components/molecules/ThreadListItem'
import { useTranslation } from '../../state/i18n'
import { useRecipients } from 'lib-components/utils/useReplyRecipients'
import { UUID } from 'lib-common/types'

interface ThreadViewProps {
  accountId: UUID
  thread: MessageThread
  onBack: () => void
}

export const ThreadView = React.memo(function ThreadView({
  accountId,
  thread: { id: threadId, messages, title },
  onBack
}: ThreadViewProps) {
  const { i18n } = useTranslation()

  const { getReplyContent, sendReply, selectedSender, setReplyContent } =
    useContext(MessageContext)

  const onUpdateContent = useCallback(
    (content) => setReplyContent(threadId, content),
    [setReplyContent, threadId]
  )

  const { recipients } = useRecipients(messages, accountId)

  const replyContent = getReplyContent(threadId)

  const onSubmitReply = useCallback(() => {
    replyContent.length > 0 &&
      selectedSender?.id &&
      sendReply({
        content: replyContent,
        messageId: messages.slice(-1)[0].id,
        recipientAccountIds: recipients
          .filter((r) => r.selected)
          .map((r) => r.id),
        accountId: selectedSender.id
      })
  }, [replyContent, selectedSender, sendReply, recipients, messages])

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
          accountId={accountId}
        />
      ))}
      <ThreadViewReply>
        <InputField
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
    </ThreadViewMobile>
  )
})

function SingleMessage({
  message,
  accountId
}: {
  message: Message
  accountId: UUID
}) {
  const ourMessage = message.sender.id === accountId
  return (
    <MessageContainer ours={ourMessage} data-qa={'single-message'}>
      <TitleRow>
        <SenderName>{message.sender.name}</SenderName>
        <SentDate white={ourMessage}>{formatTime(message.sentAt)}</SentDate>
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
  margin: ${defaultMargins.s};
  margin-bottom: 0;
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
  padding-bottom: ${defaultMargins.XXL};
`

const ThreadViewTopbar = styled.div`
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

const ThreadViewReply = styled.div`
  position: fixed;
  align-items: center;
  bottom: ${defaultMargins.xs};
  left: ${defaultMargins.s};
  right: ${defaultMargins.s};
  display: flex;
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
