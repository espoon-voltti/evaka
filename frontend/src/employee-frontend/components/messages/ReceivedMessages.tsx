// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import { MessageThread } from 'lib-common/api-types/messaging/message'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import React from 'react'
import { UUID } from '../../types'
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

const getUniqueParticipants: (t: MessageThread) => UUID[] = (
  t: MessageThread
) =>
  Object.values(
    t.messages.reduce((acc, msg) => {
      acc[msg.senderId] = msg.senderName
      msg.recipients.forEach((rec) => (acc[rec.id] = rec.name))
      return acc
    }, {})
  )

interface Props {
  accountId: UUID
  messages: Result<MessageThread[]>
  onSelectThread: (thread: MessageThread | undefined) => void
}

export function ReceivedMessages({
  accountId,
  messages,
  onSelectThread
}: Props) {
  return messages.mapAll({
    failure() {
      return <ErrorSegment />
    },
    loading() {
      return <SpinnerSegment />
    },
    success(threads) {
      return (
        <>
          {threads.map((t) => {
            const unread = t.messages.some(
              (m) => !m.readAt && m.senderId != accountId
            )
            const lastMessage = t.messages[t.messages.length - 1]
            return (
              <MessageRow
                key={t.id}
                unread={unread}
                onClick={() => onSelectThread(t)}
              >
                <ParticipantsAndPreview>
                  <Participants unread={unread}>
                    {getUniqueParticipants(t).join(', ')} {t.messages.length}
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
  })
}
