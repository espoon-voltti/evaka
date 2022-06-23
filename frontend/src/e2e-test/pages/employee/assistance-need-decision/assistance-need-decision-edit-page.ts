// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from '../../../utils'
import { Page } from '../../../utils/page'

export default class AssistanceNeedDecisionEditPage {
  constructor(private readonly page: Page) {}

  readonly #decisionMakerSelect = this.page.findByDataQa(
    'decision-maker-select'
  )

  async assertDeciderSelectVisible() {
    await this.#decisionMakerSelect.waitUntilVisible()
  }

  async assertDecisionStatus(status: string) {
    await waitUntilEqual(
      () => this.page.findByDataQa('decision-status').innerText,
      status
    )
  }

  async assertDecisionNumber(decisionNumber: number | null) {
    await waitUntilEqual(
      () => this.page.findByDataQa('decision-number').innerText,
      `${decisionNumber ?? 'null'}`
    )
  }
}
