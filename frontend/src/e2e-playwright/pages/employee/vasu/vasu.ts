// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { RawElement } from '../../../utils/element'

export default class VasuPage {
  constructor(private readonly page: Page) {}

  readonly #followupQuestion = this.page.locator(
    '[data-qa="vasu-followup-question"]'
  )
  readonly #finalizeButton = new RawElement(
    this.page,
    '[data-qa="transition-button-MOVED_TO_READY"]'
  )
  readonly #modalOkButton = new RawElement(this.page, '[data-qa="modal-okBtn"]')
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

  get followupQuestionCount(): Promise<number> {
    return this.page
      .waitForLoadState('networkidle')
      .then(() => this.#followupQuestion.count())
  }

  get finalizeButton(): RawElement {
    return this.#finalizeButton
  }

  get modalOkButton(): RawElement {
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
}
