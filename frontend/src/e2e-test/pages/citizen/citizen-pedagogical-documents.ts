// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Page } from '../../utils/page'

export default class CitizenPedagogicalDocumentsPage {
  constructor(private readonly page: Page) {}

  readonly #date = (id: string) =>
    this.page.findByDataQa(`pedagogical-document-date-${id}`)
  readonly #description = (id: string) =>
    this.page.findByDataQa(`pedagogical-document-description-${id}`)
  readonly #downloadAttachment = (id: string) =>
    this.page.findByDataQa(`attachment-${id}-download`)

  async assertPedagogicalDocumentExists(
    id: string,
    expectedDate: string,
    expectedDescription: string
  ) {
    await this.#date(id).assertTextEquals(expectedDate)
    await this.#description(id).assertTextEquals(expectedDescription)
  }

  async downloadAttachment(id: string) {
    await this.#downloadAttachment(id).click()
  }
}
