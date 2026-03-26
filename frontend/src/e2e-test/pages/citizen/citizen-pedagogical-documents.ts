// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { expect } from '../../playwright'
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
    await expect(this.#date(id)).toHaveText(expectedDate, {
      useInnerText: true
    })
    await expect(this.#description(id)).toHaveText(expectedDescription, {
      useInnerText: true
    })
  }

  async downloadAttachment(id: string) {
    await this.#downloadAttachment(id).click()
  }
}
