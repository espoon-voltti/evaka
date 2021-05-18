// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { desktopMin } from 'lib-components/breakpoints'
import { H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import React from 'react'
import styled from 'styled-components'
import { useTranslation } from '../localization'
import { formatDate } from '../util'
import { MessageTypeChip } from './MessageTypeChip'
import { Message, MessageThread, MessageType } from './types'

const MessageContainer = styled.div`
  background-color: ${colors.greyscale.white};
  padding: ${defaultMargins.s};

  @media (min-width: ${desktopMin}) {
    padding: ${defaultMargins.L};
  }

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
  const i18n = useTranslation()
  return (
    <MessageContainer>
      {title && type && (
        <TitleRow>
          <H2>{title}</H2>
          <MessageTypeChip type={type} labels={i18n.messages.types} />
        </TitleRow>
      )}
      <TitleRow>
        <SenderName>{message.senderName}</SenderName>
        <SentDate>{formatDate(message.sentAt)}</SentDate>
      </TitleRow>
      <MessageContent>{message.content}</MessageContent>
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
  thread: MessageThread
}

export default React.memo(function ThreadReadView({ thread }: Props) {
  return (
    <ThreadContainer>
      {thread.messages.map((message, idx) => (
        <Message
          key={message.id}
          message={message}
          title={idx === 0 ? thread.title : undefined}
          type={idx === 0 ? thread.type : undefined}
        />
      ))}
    </ThreadContainer>
  )
})
