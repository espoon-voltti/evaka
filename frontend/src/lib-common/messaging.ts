// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  DraftContent,
  SentMessage
} from 'lib-common/generated/api-types/messaging'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'

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
