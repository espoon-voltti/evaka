// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import React from 'react'
import styled from 'styled-components'
import { useTranslation } from '../localization'
import { formatDate } from '../util'
import { MessageTypeChip } from './MessageTypeChip'
import { MessageThread } from './types'

interface Props {
  thread: MessageThread
  active: boolean
  onClick: () => void
}

const hasUnreadMessages = (thread: MessageThread) =>
  thread.messages.some((m) => !m.readAt)

export default React.memo(function ThreadListItem({
  thread,
  active,
  onClick
}: Props) {
  const i18n = useTranslation()
  const lastMessage = thread.messages[thread.messages.length - 1]
  const participants = thread.messages.map((t) => t.senderName).join(', ')
  return (
    <Container
      isRead={!hasUnreadMessages(thread)}
      active={active}
      onClick={onClick}
      data-qa="message-list-item"
    >
      <FixedSpaceColumn>
        <Header>
          <Truncated data-qa="message-participants">{participants}</Truncated>
          <MessageTypeChip type={thread.type} labels={i18n.messages.types} />
        </Header>
        <TitleAndDate>
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
    </Container>
  )
})

const Container = styled.button<{ isRead: boolean; active: boolean }>`
  text-align: left;
  width: 100%;
  outline: none;

  background-color: ${colors.greyscale.white};
  padding: ${defaultMargins.s} ${defaultMargins.m};
  cursor: pointer;

  border: 1px solid ${colors.greyscale.lighter};

  &:focus {
    border: 2px solid ${colors.accents.petrol};
    margin: -1px 0;
    padding: ${defaultMargins.s} calc(${defaultMargins.m} - 1px);
  }

  ${(p) =>
    !p.isRead
      ? `
    border-left-color: ${colors.brandEspoo.espooTurquoise} !important;
    border-left-width: 6px !important;
    padding-left: calc(${defaultMargins.m} - 6px) !important;
  `
      : ''}

  ${(p) =>
    p.active
      ? `background-color: ${colors.brandEspoo.espooTurquoiseLight};`
      : ''}
`

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  font-weight: bold;
  font-size: 16px;
  margin-bottom: 12px;
`

const TitleAndDate = styled.div`
  display: flex;
  justify-content: space-between;
  font-weight: 600;
  margin-bottom: ${defaultMargins.xxs};
`

const Truncated = styled.span`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  :not(:last-child) {
    margin-right: ${defaultMargins.s};
  }
`
