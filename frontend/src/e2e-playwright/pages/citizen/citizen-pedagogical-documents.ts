// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { waitUntilEqual } from '../../utils'

export default class CitizenPedagogicalDocumentsPage {
  constructor(private readonly page: Page) {}

  readonly #date = (id: string) =>
    this.page.locator(`[data-qa="pedagogical-document-date-${id}"]`)
  readonly #description = (id: string) =>
    this.page.locator(`[data-qa="pedagogical-document-description-${id}"]`)

  async assertPedagogicalDocumentExists(
    id: string,
    expectedDate: string,
    expectedDescription: string
  ) {
    await waitUntilEqual(() => this.#date(id).innerText(), expectedDate)
    await waitUntilEqual(
      () => this.#description(id).innerText(),
      expectedDescription
    )
  }
}
