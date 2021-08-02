// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from 'lib-common/json'
import { VasuDocumentEvent, VasuDocumentState } from './api'

export const getLastPublished = (
  events: VasuDocumentEvent[]
): Date | undefined =>
  events.reduce<Date | undefined>((acc, event) => {
    if (event.eventType !== 'PUBLISHED') {
      return acc
    }
    if (!acc) {
      return event.created
    }
    return event.created > acc ? event.created : acc
  }, undefined)

export const getDocumentState = (
  events: JsonOf<VasuDocumentEvent>[]
): VasuDocumentState =>
  events.reduce<VasuDocumentState>((acc, { eventType }) => {
    switch (eventType) {
      case 'PUBLISHED':
        return acc
      case 'MOVED_TO_READY':
      case 'RETURNED_TO_READY':
        return 'READY'
      case 'MOVED_TO_REVIEWED':
      case 'RETURNED_TO_REVIEWED':
        return 'REVIEWED'
      case 'MOVED_TO_CLOSED':
        return 'CLOSED'
    }
  }, 'DRAFT')
