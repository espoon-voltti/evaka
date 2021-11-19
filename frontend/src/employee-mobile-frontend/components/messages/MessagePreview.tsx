// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import {
  Container,
  Header,
  TitleAndDate,
  Truncated
} from 'lib-components/molecules/ThreadListItem'
import { MessageThread } from 'lib-common/generated/api-types/messaging'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { formatDate } from 'lib-common/date'
import LocalDate from 'lib-common/local-date'
import React from 'react'

export function MessagePreview({
  thread,
  hasUnreadMessages,
  onClick
}: {
  thread: MessageThread
  hasUnreadMessages: boolean
  onClick: () => void
}) {
  const lastMessage = thread.messages[thread.messages.length - 1]
  const participants = [...new Set(thread.messages.map((t) => t.sender.name))]
  return (
    <MessagePreviewContainer
      isRead={!hasUnreadMessages}
      active={false}
      data-qa="message-preview"
      onClick={onClick}
    >
      <FixedSpaceColumn>
        <Header isRead={!hasUnreadMessages}>
          <Truncated data-qa="message-participants">
            {participants.join(', ')}
          </Truncated>
        </Header>
        <TitleAndDate isRead={!hasUnreadMessages}>
          <Truncated>{thread.title}</Truncated>
          <span>
            {formatDate(
              lastMessage.sentAt,
              LocalDate.fromSystemTzDate(lastMessage.sentAt).isEqual(
                LocalDate.today()
              )
                ? 'HH:mm'
                : 'd.M.'
            )}
          </span>
        </TitleAndDate>
        <Truncated>
          {lastMessage.content.substring(0, 200).replace('\n', ' ')}
        </Truncated>
      </FixedSpaceColumn>
    </MessagePreviewContainer>
  )
}

const MessagePreviewContainer = styled(Container)`
  border-left-width: 0;
  border-right-width: 0;
  border-top-width: 0;
`
