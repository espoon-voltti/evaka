// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, TextInput } from '../../../utils/page'

export default class VasuPage {
  constructor(private readonly page: Page) {}

  readonly #followupQuestions = this.page.findAll(
    '[data-qa="vasu-followup-question"]'
  )
  readonly finalizeButton = this.page.find(
    '[data-qa="transition-button-MOVED_TO_READY"]'
  )
  readonly modalOkButton = this.page.find('[data-qa="modal-okBtn"]')
  readonly #firstDocumentSection = this.page
    .findAll('[data-qa="vasu-document-section"]')
    .first()

  readonly #followupNewInput = new TextInput(
    this.page.findAll('[data-qa="vasu-followup-entry-new-input"]').first()
  )
  readonly #followupNewSaveButton = this.page.find(
    '[data-qa="vasu-followup-entry-new-submit"]'
  )
  readonly #followupEntryTexts = this.page.findAll(
    '[data-qa="vasu-followup-entry-text"]'
  )
  readonly #followupEntryMetadatas = this.page.findAll(
    '[data-qa="vasu-followup-entry-metadata"]'
  )
  readonly #followupEntryEditButtons = this.page.findAll(
    '[data-qa="vasu-followup-entry-edit-btn"]'
  )
  readonly #followupEntryInput = new TextInput(
    this.page.find('[data-qa="vasu-followup-entry-edit-input"]')
  )
  readonly #followupEntrySaveButton = this.page.find(
    '[data-qa="vasu-followup-entry-edit-submit"]'
  )

  get followupQuestionCount(): Promise<number> {
    return this.#followupQuestions.count()
  }

  async assertDocumentVisible() {
    await this.#firstDocumentSection.waitUntilVisible()
  }

  async inputFollowupComment(comment: string) {
    await this.#followupNewInput.type(comment)
    await this.#followupNewSaveButton.click()
  }

  get followupEntryTexts(): Promise<Array<string>> {
    return this.#followupEntryTexts.allInnerTexts()
  }

  get followupEntryMetadata(): Promise<Array<string>> {
    return this.#followupEntryMetadatas.allInnerTexts()
  }

  async editFollowupComment(ix: number, text: string) {
    await this.#followupEntryEditButtons.nth(ix).click()
    await this.#followupEntryInput.type(text)
    await this.#followupEntrySaveButton.click()
  }
}
