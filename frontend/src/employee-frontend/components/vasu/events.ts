// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { VasuDocumentEvent } from './api'

export const getLastPublished = (events: VasuDocumentEvent[]) =>
  events.reduce<Date | undefined>((acc, event) => {
    if (event.eventType !== 'PUBLISHED') {
      return acc
    }
    if (!acc) {
      return event.created
    }
    return event.created > acc ? event.created : acc
  }, undefined)
