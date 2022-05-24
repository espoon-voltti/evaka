// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from '../../../utils'
import { Collapsible, Page } from '../../../utils/page'

export class ChildDocumentsPage {
  constructor(readonly page: Page) {}

  readonly vasuCollapsible = new Collapsible(
    this.page.findByDataQa('vasu-and-leops-collapsible')
  )
  readonly #vasuRowStateChip = (vasuId: string) =>
    this.page.findByDataQa(`state-chip-${vasuId}`)
  readonly #vasuRowPublishedAt = (vasuId: string) =>
    this.page.findByDataQa(`published-at-${vasuId}`)

  async assertVasuRow(
    vasuId: string,
    expectedStatus: string,
    expectedPublishedAt: string
  ) {
    await waitUntilEqual(
      () => this.#vasuRowStateChip(vasuId).textContent,
      expectedStatus
    )
    await waitUntilEqual(
      () => this.#vasuRowPublishedAt(vasuId).textContent,
      expectedPublishedAt
    )
  }
}
