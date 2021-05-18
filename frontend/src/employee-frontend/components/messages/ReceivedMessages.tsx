import { isToday } from 'date-fns'
import { Result } from 'lib-common/api'
import Loader from 'lib-components/atoms/Loader'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { espooBrandColors, greyscale } from 'lib-components/colors'
import { defaultMargins } from 'lib-components/white-space'
import React from 'react'
import styled from 'styled-components'
import { DATE_FORMAT_NO_YEAR, DATE_FORMAT_TIME_ONLY } from '../../constants'
import { formatDate } from '../../utils/date'
import { MessageTypeChip } from './MessageTypeChip'
import { MessageThread } from './types'

const MessageRow = styled.div<{ unread: boolean }>`
  display: flex;
  justify-content: space-between;
  padding: ${defaultMargins.s};
  cursor: pointer;
  :first-child {
    border-top: 1px solid ${greyscale.lighter};
  }
  border-bottom: 1px solid ${greyscale.lighter};
  border-left: ${(p) =>
    `6px solid ${p.unread ? espooBrandColors.espooTurquoise : 'transparent'}`};
`
const Participants = styled.div<{ unread: boolean }>`
  color: ${(p) => (p.unread ? greyscale.darkest : greyscale.dark)};
  font-weight: 600;
`
const Truncated = styled.div`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`
const Title = styled.span<{ unread: boolean }>`
  font-weight: ${(p) => (p.unread ? 600 : 400)};
`
const FirstColumn = styled.div`
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-right: ${defaultMargins.m};
`
const SecondColumn = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-end;
`

function formatSentAt(sentAt: Date) {
  const format = isToday(sentAt) ? DATE_FORMAT_TIME_ONLY : DATE_FORMAT_NO_YEAR
  return formatDate(sentAt, format)
}

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
            <FirstColumn>
              <Participants unread={unread}>
                {t.messages.map((m) => m.senderName).join(', ')}{' '}
                {t.messages.length}
              </Participants>
              <Truncated>
                <Title unread={unread}>{t.title}</Title>
                {' - '}
                {lastMessage.content}
              </Truncated>
            </FirstColumn>
            <SecondColumn>
              <MessageTypeChip type={t.type} />
              {formatSentAt(lastMessage.sentAt)}
            </SecondColumn>
          </MessageRow>
        )
      })}
    </>
  )
}
