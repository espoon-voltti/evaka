// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { SyntheticEvent, useCallback } from 'react'
import { Link } from 'react-router-dom'

import { formatDateOrTime } from 'lib-common/date'
import {
  CitizenMessageThread,
  MessageAccount
} from 'lib-common/generated/api-types/messaging'
import { ScreenReaderOnly } from 'lib-components/atoms/ScreenReaderOnly'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { MessageCharacteristics } from 'lib-components/messages/MessageCharacteristics'
import {
  Container,
  DeleteThreadButton,
  Header,
  TitleAndDate,
  Truncated
} from 'lib-components/messages/ThreadListItem'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'

import { getAttachmentUrl } from '../attachments'
import { useTranslation } from '../localization'

import { isRegularThread } from './state'

interface Props {
  thread: CitizenMessageThread
  active: boolean
  hasUnreadMessages: boolean
  onClick: () => void
  onDelete: () => void
}

export default React.memo(function ThreadListItem({
  thread,
  active,
  hasUnreadMessages,
  onClick,
  onDelete
}: Props) {
  const i18n = useTranslation()

  const formatSenderName = ({ name, type }: MessageAccount) =>
    type === 'GROUP' ? `${name} (${i18n.messages.staffAnnotation})` : name

  const lastMessage = isRegularThread(thread)
    ? thread.messages[thread.messages.length - 1]
    : null
  const participants = isRegularThread(thread)
    ? [...new Set(thread.messages.map((m) => formatSenderName(m.sender)))].join(
        ', '
      )
    : thread.sender
    ? formatSenderName(thread.sender)
    : ''

  const handleDelete = useCallback(
    (e: SyntheticEvent<HTMLButtonElement>) => {
      e.stopPropagation()
      onDelete()
    },
    [onDelete]
  )

  const lastMessageSentAt = isRegularThread(thread)
    ? thread.messages[thread.messages.length - 1].sentAt
    : thread.lastMessageSentAt
  return (
    <Container
      isRead={!hasUnreadMessages}
      active={active}
      onClick={onClick}
      onKeyDown={(e) => e.key === 'Enter' && onClick()}
      data-qa="thread-list-item"
      tabIndex={0}
    >
      <FixedSpaceColumn>
        <Header isRead={!hasUnreadMessages}>
          <Truncated data-qa="message-participants">
            <ScreenReaderOnly>
              {i18n.messages.threadList.participants}:
            </ScreenReaderOnly>
            {participants}
          </Truncated>
          <FixedSpaceRow>
            {isRegularThread(thread) && (
              <DeleteThreadButton
                aria-label={i18n.common.delete}
                data-qa="delete-thread-btn"
                onClick={handleDelete}
              />
            )}
            <MessageCharacteristics
              type={isRegularThread(thread) ? thread.messageType : 'MESSAGE'}
              urgent={thread.urgent}
              sensitive={!isRegularThread(thread) || thread.sensitive}
            />
          </FixedSpaceRow>
        </Header>
        <ScreenReaderOnly>
          <Link to={`/messages/${thread.id}`} tabIndex={-1}>
            {i18n.messages.openMessage}
          </Link>
        </ScreenReaderOnly>
        <TitleAndDate isRead={!hasUnreadMessages}>
          <Truncated>
            <ScreenReaderOnly>
              {i18n.messages.threadList.title}:
            </ScreenReaderOnly>
            {isRegularThread(thread) ? thread.title : i18n.messages.sensitive}
          </Truncated>
          <div>
            <ScreenReaderOnly>
              {i18n.messages.threadList.sentAt}:
            </ScreenReaderOnly>
            {lastMessageSentAt && (
              <time dateTime={lastMessageSentAt.formatIso()}>
                {formatDateOrTime(lastMessageSentAt)}
              </time>
            )}
          </div>
        </TitleAndDate>
        <Truncated>
          <ScreenReaderOnly>
            {i18n.messages.threadList.message}:
          </ScreenReaderOnly>
          {isRegularThread(thread) ? (
            lastMessage &&
            lastMessage.content
              .substring(0, 200)
              .replace(new RegExp('\\n', 'g'), ' ')
          ) : (
            <i>{i18n.messages.strongAuthRequired}</i>
          )}
        </Truncated>
        {isRegularThread(thread) &&
          lastMessage &&
          lastMessage.attachments.length > 0 && (
            <FixedSpaceColumn spacing="xs">
              {lastMessage.attachments.map((attachment) => (
                <FileDownloadButton
                  key={attachment.id}
                  file={attachment}
                  getFileUrl={getAttachmentUrl}
                  icon
                  data-qa="thread-list-attachment"
                />
              ))}
            </FixedSpaceColumn>
          )}
      </FixedSpaceColumn>
    </Container>
  )
})
