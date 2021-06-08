// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import Loader from 'lib-components/atoms/Loader'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import React, { ReactNode, useState } from 'react'
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
import { SentMessage } from './types'

function MessageContent({
  children,
  expanded
}: {
  children: ReactNode
  expanded: boolean
}) {
  return expanded ? <div>{children}</div> : <Truncated>{children}</Truncated>
}

function SentMessage({ message: message }: { message: SentMessage }) {
  const [expanded, setExpanded] = useState(false)
  return (
    <MessageRow onClick={() => setExpanded(!expanded)}>
      <ParticipantsAndPreview>
        <Participants>{message.recipientNames.join(', ')}</Participants>
        <MessageContent expanded={expanded}>
          <Title>{message.threadTitle}</Title>
          {' - '}
          {message.content}
        </MessageContent>
      </ParticipantsAndPreview>
      <TypeAndDate>
        <MessageTypeChip type={message.type} />
        <SentAt sentAt={message.sentAt} />
      </TypeAndDate>
    </MessageRow>
  )
}

interface Props {
  messages: Result<SentMessage[]>
}

export function SentMessages({ messages }: Props) {
  return messages.mapAll({
    loading() {
      return <Loader />
    },
    failure() {
      return <ErrorSegment />
    },
    success(messages) {
      return (
        <>
          {messages.map((msg) => (
            <SentMessage key={msg.contentId} message={msg} />
          ))}
        </>
      )
    }
  })
}
