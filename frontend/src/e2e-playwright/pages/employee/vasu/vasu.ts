// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Locator, Page } from 'playwright'

export default class VasuPage {
  constructor(private readonly page: Page) {}

  readonly #followupQuestion = this.page.locator(
    '[data-qa="vasu-followup-question"]'
  )
  readonly #finalizeButton = this.page.locator(
    '[data-qa="transition-button-MOVED_TO_READY"]'
  )
  readonly #modalOkButton = this.page.locator('[data-qa="modal-okBtn"]')
  readonly #documentSection = this.page.locator(
    '[data-qa="vasu-document-section"]'
  )

  readonly #followupInput = this.page.locator('[data-qa="vasu-followup-input"]')
  readonly #followupAddBtn = this.page.locator(
    '[data-qa="vasu-followup-addBtn"]'
  )
  readonly #followupEntryText = this.page.locator(
    '[data-qa="vasu-followup-entry-text"]'
  )
  readonly #followupEntryMetadata = this.page.locator(
    '[data-qa="vasu-followup-entry-metadata"]'
  )

  readonly #followupEntryEditBtn = this.page.locator(
    '[data-qa="vasu-followup-entry-edit-btn"]'
  )

  get followupQuestionCount(): Promise<number> {
    return this.page
      .waitForLoadState('networkidle')
      .then(() => this.#followupQuestion.count())
  }

  get finalizeButton(): Locator {
    return this.#finalizeButton
  }

  get modalOkButton(): Locator {
    return this.#modalOkButton
  }

  async assertDocumentVisible() {
    await this.#documentSection.first().waitFor({ state: 'visible' })
  }

  async inputFollowupComment(comment: string) {
    await this.#followupInput.type(comment)
    await this.#followupAddBtn.click()
    await this.page.waitForLoadState('networkidle')
  }

  get followupEntryTexts(): Promise<Array<string>> {
    return this.#followupEntryText.allInnerTexts()
  }

  get followupEntryMetadata(): Promise<Array<string>> {
    return this.#followupEntryMetadata.allInnerTexts()
  }

  async editFollowupComment(ix: number, text: string) {
    await this.#followupEntryEditBtn.nth(ix).click()
    await this.#followupInput.first().type(text)
    await this.#followupAddBtn.first().click()
    await this.page.waitForLoadState('networkidle')
  }
}
