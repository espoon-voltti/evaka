// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from '../../../utils/page'

export default class AssistanceNeedPreschoolDecisionPreviewPage {
  constructor(private readonly page: Page) {}

  get guardiansHeardOn() {
    return this.page.findByDataQa(`guardians-heard-on`).text
  }

  get viewOfGuardians() {
    return this.page.findByDataQa('view-of-guardians').text
  }

  get validFrom() {
    return this.page.findByDataQa('valid-from').text
  }

  get selectedUnit() {
    return this.page.findByDataQa('unit').text
  }

  get preparedBy1() {
    return this.page.findByDataQa('preparer-1').text
  }

  get decisionMaker() {
    return this.page.findByDataQa('decision-maker').text
  }

  readonly sendDecisionButton = this.page.findByDataQa('send-decision')
  readonly revertToUnsent = this.page.findByDataQa('revert-to-unsent')
  get decisionSentAt() {
    return this.page.findByDataQa('decision-sent-at')
  }

  async assertPageTitle(title: string): Promise<void> {
    await this.page.findByDataQa('page-title').assertTextEquals(title)
  }
}
