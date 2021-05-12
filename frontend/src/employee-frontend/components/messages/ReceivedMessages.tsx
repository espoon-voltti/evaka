import { Result } from 'lib-common/api'
import React from 'react'
import styled from 'styled-components'
import { StaticChip } from '../../../lib-components/atoms/Chip'
import Loader from '../../../lib-components/atoms/Loader'
import ErrorSegment from '../../../lib-components/atoms/state/ErrorSegment'
import {
  accentColors,
  espooBrandColors,
  greyscale
} from '../../../lib-components/colors'
import { useTranslation } from '../../state/i18n'
import { MessageThread } from './types'

const MessageRow = styled.li<{ unread: boolean }>`
  display: flex;
  justify-content: space-between;
  border-top: 1px solid ${greyscale.lighter};
  border-left: ${(p) =>
    `6px solid ${p.unread ? espooBrandColors.espooTurquoise : 'transparent'}`};
`
const Participants = styled.div<{ unread: boolean }>`
  color: ${(p) => (p.unread ? greyscale.darkest : greyscale.dark)};
`
const TitleAndPreview = styled.div`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`
const Title = styled.span<{ unread: boolean }>`
  font-weight: ${(p) => (p.unread ? 600 : 400)};
`

const chipColors = {
  MESSAGE: accentColors.yellow,
  BULLETIN: accentColors.water
}

interface Props {
  messages: Result<MessageThread[]>
}

export function ReceivedMessages({ messages }: Props) {
  const { i18n } = useTranslation()
  return (
    <div>
      {messages.mapAll({
        failure() {
          return <ErrorSegment />
        },
        loading() {
          return <Loader />
        },
        success(threads) {
          return (
            <ul>
              {threads.map((t) => {
                const unread = t.messages.some((m) => !m.readAt)
                return (
                  <MessageRow key={t.id} unread={unread}>
                    <div>
                      <Participants unread={unread}>
                        {t.messages.map((m) => m.senderName).join(', ')}{' '}
                        {t.messages.length}
                      </Participants>
                      <TitleAndPreview>
                        <Title unread={unread}>{t.title}</Title> –{' '}
                        {t.messages[-1].content}
                      </TitleAndPreview>
                    </div>
                    <div>
                      <StaticChip color={chipColors[t.type]}>
                        {i18n.messages.types[t.type]}
                      </StaticChip>
                      {t.messages[-1]?.sentAt}
                    </div>
                  </MessageRow>
                )
              })}
            </ul>
          )
        }
      })}
    </div>
  )
}
