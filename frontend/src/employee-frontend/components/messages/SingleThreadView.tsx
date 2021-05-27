// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { H2 } from 'lib-components/typography'
import React from 'react'
import styled from 'styled-components'
import InlineButton from '../../../lib-components/atoms/buttons/InlineButton'
import colors, { greyscale } from '../../../lib-components/colors'
import { ContentArea } from '../../../lib-components/layout/Container'
import { defaultMargins, Gap } from '../../../lib-components/white-space'
import { faAngleLeft } from '../../../lib-icons'
import { DATE_FORMAT_DATE_TIME } from '../../constants'
import { useTranslation } from '../../state/i18n'
import { formatDate } from '../../utils/date'
import { MessageTypeChip } from './MessageTypeChip'
import { Message, MessageThread, MessageType } from './types'

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
  color: ${greyscale.dark};
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
  goBack: () => void
  thread: MessageThread
}

export function SingleThreadView({
  goBack,
  thread: { messages, title, type }
}: Props) {
  const { i18n } = useTranslation()
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
      </ScrollContainer>
    </ThreadContainer>
  )
}
