// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import Loader from 'lib-components/atoms/Loader'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import React from 'react'
import {
  MessageRow,
  Participants,
  ParticipantsAndPreview,
  SentAt,
  Title,
  Truncated,
  TypeAndDate
} from './MessageComponents'
import { MessageTypeChip } from './MessageTypeChip'
import { MessageThread } from './types'

interface Props {
  messages: Result<MessageThread[]>
  onSelectThread: (thread: MessageThread) => void
}

export function ReceivedMessages({ messages, onSelectThread }: Props) {
  if (messages.isFailure) return <ErrorSegment />
  if (messages.isLoading) return <Loader />
  return (
    <>
      {messages.value.map((t) => {
        const unread = t.messages.some((m) => !m.readAt)
        const lastMessage = t.messages[t.messages.length - 1]
        return (
          <MessageRow
            key={t.id}
            unread={unread}
            onClick={() => onSelectThread(t)}
          >
            <ParticipantsAndPreview>
              <Participants unread={unread}>
                {t.messages.map((m) => m.senderName).join(', ')}{' '}
                {t.messages.length}
              </Participants>
              <Truncated>
                <Title unread={unread}>{t.title}</Title>
                {' - '}
                {lastMessage.content}
              </Truncated>
            </ParticipantsAndPreview>
            <TypeAndDate>
              <MessageTypeChip type={t.type} />
              <SentAt sentAt={lastMessage.sentAt} />
            </TypeAndDate>
          </MessageRow>
        )
      })}
    </>
  )
}
