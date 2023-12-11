// SPDX-FileCopyrightText: 2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Element } from '../../../utils/page'
export class TimelineEvent extends Element {
  async expandEvent() {
    await this.find('[data-qa="event-expander-button"] button').click()
  }

  assertMetadataContains(metadataContent: string) {
    return this.findByDataQa('partner-metadata').assertText((text) =>
      text.includes(metadataContent)
    )
  }
}
