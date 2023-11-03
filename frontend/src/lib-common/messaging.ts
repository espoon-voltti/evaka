// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  DraftContent,
  MessageAccount,
  MessageChild,
  SentMessage
} from 'lib-common/generated/api-types/messaging'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'

import { formatFirstName } from './names'

export const deserializeDraftContent = ({
  created,
  ...rest
}: JsonOf<DraftContent>): DraftContent => ({
  ...rest,
  created: HelsinkiDateTime.parseIso(created)
})

export const deserializeSentMessage = ({
  sentAt,
  ...rest
}: JsonOf<SentMessage>): SentMessage => ({
  ...rest,
  sentAt: HelsinkiDateTime.parseIso(sentAt)
})

export function formatAccountNames(
  sender: MessageAccount,
  recipients: MessageAccount[],
  children: MessageChild[]
): { senderName: string; recipientNames: string[] } {
  const childNames =
    children.length > 0
      ? children.map((child) => formatFirstName(child)).join(', ')
      : undefined
  const childSuffix = childNames ? ` (${childNames})` : ''

  const senderName =
    sender.name + (sender.type === 'CITIZEN' ? childSuffix : '')
  const recipientNames = recipients.map(
    (r) => r.name + (r.type === 'CITIZEN' ? childSuffix : '')
  )

  return { senderName, recipientNames }
}
