// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { VasuDocumentEvent } from 'lib-common/generated/api-types/vasu'

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
