// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { Result, wrapResult } from 'lib-common/api'
import { MessageType } from 'lib-common/generated/api-types/messaging'
import {
  MessageAccountId,
  MessageThreadId
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { MessageCharacteristics } from 'lib-components/messages/MessageCharacteristics'

import { archiveThread } from '../../generated/api-clients/messaging'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import EllipsisMenu from '../common/EllipsisMenu'

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

const archiveThreadResult = wrapResult(archiveThread)

export type ThreadListItem = {
  id: MessageThreadId
  title: string
  content: string
  urgent: boolean
  sensitive: boolean
  participants: string[]
  unread: boolean
  onClick: () => void
  type: MessageType
  timestamp?: HelsinkiDateTime
  messageCount?: number
  dataQa?: string
}

interface Props {
  items: Result<ThreadListItem[]>
  accountId: MessageAccountId
  onChangeFolder?: (item: MessageThreadId) => void
  onArchived?: () => void
}

export function ThreadList({
  items: messages,
  accountId,
  onChangeFolder,
  onArchived
}: Props) {
  const { i18n } = useTranslation()

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
            <Participants unread={item.unread} data-qa="participants">
              {item.participants.length > 0
                ? item.participants.join(', ')
                : '-'}{' '}
              {item.messageCount}
            </Participants>
            <Truncated>
              <Title unread={item.unread} data-qa="thread-list-item-title">
                {item.title}
                {item.sensitive && ` (${i18n.messages.sensitive})`}
              </Title>
              <Hyphen>{' ― '}</Hyphen>
              <span data-qa="thread-list-item-content">{item.content}</span>
            </Truncated>
          </ParticipantsAndPreview>
          <FixedSpaceRow>
            <TypeAndDate>
              <MessageCharacteristics type={item.type} urgent={item.urgent} />
              {item.timestamp && <Timestamp date={item.timestamp} />}
            </TypeAndDate>
            {}
            <EllipsisMenu
              items={[
                ...(onChangeFolder
                  ? [
                      {
                        id: 'change-folder',
                        label: i18n.messages.changeFolder.button,
                        onClick: () => onChangeFolder(item.id)
                      }
                    ]
                  : []),
                ...(onArchived
                  ? [
                      {
                        id: 'archive',
                        label: i18n.common.archive,
                        onClick: () =>
                          archiveThreadResult({
                            accountId,
                            threadId: item.id
                          }).then(onArchived)
                      }
                    ]
                  : [])
              ]}
            />
          </FixedSpaceRow>
        </MessageRow>
      ))}
    </>
  ))
}
