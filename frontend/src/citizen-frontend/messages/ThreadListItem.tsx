// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import React from 'react'
import styled from 'styled-components'
import { MessageThread } from 'lib-common/generated/api-types/messaging'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import { fontWeights } from 'lib-components/typography'
import { useTranslation } from '../localization'
import { MessageTypeChip } from './MessageTypeChip'
import { formatDate } from 'lib-common/date'
import { getAttachmentBlob } from '../attachments'

interface Props {
  thread: MessageThread
  active: boolean
  hasUnreadMessages: boolean
  onClick: () => void
  onAttachmentUnavailable: () => void
}

export default React.memo(function ThreadListItem({
  thread,
  active,
  hasUnreadMessages,
  onClick,
  onAttachmentUnavailable
}: Props) {
  const i18n = useTranslation()
  const lastMessage = thread.messages[thread.messages.length - 1]
  const participants = [...new Set(thread.messages.map((t) => t.sender.name))]
  return (
    <Container
      isRead={!hasUnreadMessages}
      active={active}
      onClick={onClick}
      data-qa="thread-list-item"
    >
      <FixedSpaceColumn>
        <Header isRead={!hasUnreadMessages}>
          <Truncated data-qa="message-participants">
            {participants.join(', ')}
          </Truncated>
          <MessageTypeChip type={thread.type} labels={i18n.messages.types} />
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
        {lastMessage.attachments.length > 0 && (
          <FixedSpaceColumn spacing="xs">
            {lastMessage.attachments.map((attachment) => (
              <FileDownloadButton
                key={attachment.id}
                file={attachment}
                fileFetchFn={getAttachmentBlob}
                onFileUnavailable={onAttachmentUnavailable}
                icon
                data-qa="thread-list-attachment"
                openInBrowser={true}
              />
            ))}
          </FixedSpaceColumn>
        )}
      </FixedSpaceColumn>
    </Container>
  )
})

const Container = styled.div<{ isRead: boolean; active: boolean }>`
  text-align: left;
  width: 100%;

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

const Header = styled.div<{ isRead: boolean }>`
  display: flex;
  justify-content: space-between;
  font-weight: ${({ isRead }) =>
    isRead ? fontWeights.normal : fontWeights.semibold};
  font-size: 16px;
  margin-bottom: 12px;
`

const TitleAndDate = styled.div<{ isRead: boolean }>`
  display: flex;
  justify-content: space-between;
  font-weight: ${({ isRead }) =>
    isRead ? fontWeights.normal : fontWeights.semibold};
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
