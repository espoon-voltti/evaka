// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Result } from 'lib-common/api'
import { MessageType } from 'lib-common/generated/enums'
import { UUID } from 'lib-common/types'
import { renderResult } from '../async-rendering'
import {
  Hyphen,
  MessageRow,
  Participants,
  ParticipantsAndPreview,
  Timestamp,
  Title,
  Truncated,
  TypeAndDate
} from './MessageComponents'
import { MessageTypeChip } from './MessageTypeChip'

export type ThreadListItem = {
  id: UUID
  title: string
  content: string
  participants: string[]
  unread: boolean
  onClick: () => void
  type: MessageType
  timestamp?: Date
  messageCount?: number
  dataQa?: string
}

interface Props {
  items: Result<ThreadListItem[]>
}

export function ThreadList({ items: messages }: Props) {
  return renderResult(messages, (threads) => (
    <>
      {threads.map((item) => (
        <MessageRow
          key={item.id}
          unread={item.unread}
          onClick={item.onClick}
          data-qa={item.dataQa}
        >
          <ParticipantsAndPreview>
            <Participants unread={item.unread}>
              {item.participants.length > 0
                ? item.participants.join(', ')
                : '-'}{' '}
              {item.messageCount}
            </Participants>
            <Truncated>
              <Title unread={item.unread} data-qa="thread-list-item-title">
                {item.title}
              </Title>
              <Hyphen>{' ― '}</Hyphen>
              <span data-qa="thread-list-item-content">{item.content}</span>
            </Truncated>
          </ParticipantsAndPreview>
          <TypeAndDate>
            <MessageTypeChip type={item.type} />
            {item.timestamp && <Timestamp date={item.timestamp} />}
          </TypeAndDate>
        </MessageRow>
      ))}
    </>
  ))
}
