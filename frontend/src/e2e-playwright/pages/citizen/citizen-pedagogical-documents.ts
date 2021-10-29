// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { waitUntilEqual, waitUntilFalse, waitUntilTrue } from '../../utils'

export default class CitizenPedagogicalDocumentsPage {
  constructor(private readonly page: Page) {}

  readonly #date = (id: string) =>
    this.page.locator(`[data-qa="pedagogical-document-date-${id}"]`)
  readonly #childName = (id: string) =>
    this.page.locator(`[data-qa="pedagogical-document-child-name-${id}"]`)
  readonly #description = (id: string) =>
    this.page.locator(`[data-qa="pedagogical-document-description-${id}"]`)
  readonly #downloadAttachment = (id: string) =>
    this.page.locator(
      `[data-qa="pedagogical-document-attachment-download-${id}"]`
    )
  readonly #unreadDocumentCount = this.page.locator(
    '[data-qa="unread-pedagogical-documents-count"]'
  )

  async assertUnreadPedagogicalDocumentIndicatorCount(expectedCount: number) {
    await waitUntilEqual(
      () => this.#unreadDocumentCount.innerText(),
      expectedCount.toString()
    )
  }

  async assertUnreadPedagogicalDocumentIndicatorIsNotShown() {
    await waitUntilFalse(() => this.#unreadDocumentCount.isVisible())
  }

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

  async downloadAttachment(id: string) {
    await this.#downloadAttachment(id).click()
  }

  async assertChildNameIsNotShown(id: string) {
    await waitUntilFalse(() => this.#childName(id).isVisible())
  }

  async assertChildNameIsShown(id: string) {
    await waitUntilTrue(() => this.#childName(id).isVisible())
  }

  async assertChildNameIs(id: string, expectedName: string) {
    await waitUntilEqual(() => this.#childName(id).innerText(), expectedName)
  }
}
