// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { formatDateOrTime } from 'lib-common/date'
import { MessageThread } from 'lib-common/generated/api-types/messaging'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import FileDownloadButton from 'lib-components/molecules/FileDownloadButton'
import {
  Container,
  Header,
  TitleAndDate,
  Truncated
} from 'lib-components/molecules/ThreadListItem'
import React from 'react'
import { getAttachmentBlob } from '../attachments'
import { useTranslation } from '../localization'
import { MessageTypeChip } from './MessageTypeChip'

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
          <span>{formatDateOrTime(lastMessage.sentAt)}</span>
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
