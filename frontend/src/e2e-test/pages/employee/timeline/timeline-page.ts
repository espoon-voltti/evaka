// SPDX-FileCopyrightText: 2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from '../../../utils/page'

import { TimelineEvent } from './timeline-event'
export class TimelinePage {
  constructor(private readonly page: Page) {}

  getTimelineEvent(type: string, index: number) {
    const elem = this.page
      .findAll(`[data-qa="timeline-event-${type}"]`)
      .nth(index)
    return new TimelineEvent(elem)
  }
}
