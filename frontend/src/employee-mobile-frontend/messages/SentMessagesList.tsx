// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'

import { formatDateOrTime } from 'lib-common/date'
import { SentMessage } from 'lib-common/generated/api-types/messaging'
import { usePagedInfiniteQueryResult } from 'lib-common/query'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { MessageCharacteristics } from 'lib-components/messages/MessageCharacteristics'
import {
  Container,
  Header,
  TitleAndDate,
  Truncated
} from 'lib-components/messages/ThreadListItem'
import { defaultMargins } from 'lib-components/white-space'

import { renderResult } from '../async-rendering'
import { useTranslation } from '../common/i18n'

import { sentMessagesQuery } from './queries'
import { MessageContext } from './state'

interface Props {
  onSelectMessage: (message: SentMessage) => void
}

export default React.memo(function SentMessagesList({
  onSelectMessage
}: Props) {
  const { i18n } = useTranslation()
  const { selectedAccount } = useContext(MessageContext)

  const { data: sentMessages } = usePagedInfiniteQueryResult(
    sentMessagesQuery(selectedAccount?.account.id ?? ''),
    { enabled: selectedAccount !== undefined }
  )

  return renderResult(sentMessages, (messages) => (
    <div>
      {messages.length > 0 ? (
        messages.map((message) => (
          <SentMessagePreview
            key={message.contentId}
            message={message}
            onClick={() => onSelectMessage(message)}
          />
        ))
      ) : (
        <NoSentMessages>{i18n.messages.noSentMessages}</NoSentMessages>
      )}
    </div>
  ))
})

const NoSentMessages = styled.div`
  padding: ${defaultMargins.s};
  text-align: center;
`

const SentMessagePreview = React.memo(function SentMessagePreview({
  message,
  onClick
}: {
  message: SentMessage
  onClick: () => void
}) {
  return (
    <Container
      isRead={true}
      active={false}
      data-qa="sent-message-preview"
      onClick={onClick}
    >
      <FixedSpaceColumn>
        <Header isRead={true}>
          <Truncated data-qa="message-recipients">
            {message.recipientNames.join(', ')}
          </Truncated>
          <MessageCharacteristics type={message.type} urgent={message.urgent} />
        </Header>
        <TitleAndDate isRead={true}>
          <Truncated data-qa="message-preview-title">
            {message.threadTitle}
          </Truncated>
          <span>{formatDateOrTime(message.sentAt)}</span>
        </TitleAndDate>
        <Truncated>
          {message.content
            .substring(0, 200)
            .replace(new RegExp('\\n', 'g'), ' ')}
        </Truncated>
      </FixedSpaceColumn>
    </Container>
  )
})
