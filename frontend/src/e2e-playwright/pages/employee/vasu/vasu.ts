// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilFalse } from 'e2e-playwright/utils'
import { Page, TextInput, Element } from '../../../utils/page'

export class AuthorsSection extends Element {
  readonly #primaryInputs = this.findAll(
    '[data-qa="multi-field-question"] input'
  )
  primaryFirstNameInput = new TextInput(this.#primaryInputs.nth(0))
  primaryLastNameInput = new TextInput(this.#primaryInputs.nth(1))
  primaryTitleInput = new TextInput(this.#primaryInputs.nth(2))
  primaryPhoneNumberInput = new TextInput(this.#primaryInputs.nth(3))

  readonly #otherFieldsRows = this.findAll(
    '[data-qa="multi-field-list-question"] [data-qa="field-row"]'
  )
  readonly otherFieldsInputs = (ix: number) =>
    this.#otherFieldsRows.nth(ix).findAll('input')
  otherFirstNameInput = (ix: number) =>
    new TextInput(this.otherFieldsInputs(ix).nth(0))
  otherLastNameInput = (ix: number) =>
    new TextInput(this.otherFieldsInputs(ix).nth(1))
  otherTitleInput = (ix: number) =>
    new TextInput(this.otherFieldsInputs(ix).nth(2))
  otherPhoneNumberInput = (ix: number) =>
    new TextInput(this.otherFieldsInputs(ix).nth(3))

  readonly #primaryValue = this.findAll(
    '[data-qa="multi-field-question"] [data-qa="value-or-no-record"]'
  ).first()

  readonly #otherFieldsValues = this.findAll(
    '[data-qa="value-or-no-record"]'
  ).nth(1)

  get primaryValue(): Promise<string> {
    return this.#primaryValue.innerText
  }

  get otherFieldsCount(): Promise<number> {
    return this.#otherFieldsRows.count()
  }

  get otherValues(): Promise<string> {
    return this.#otherFieldsValues.innerText
  }
}

class VasuPageCommon {
  constructor(readonly page: Page) {}

  readonly #documentSection = this.page.findAll(
    '[data-qa="vasu-document-section"]'
  )
  readonly #followupQuestions = this.page.findAll(
    '[data-qa="vasu-followup-question"]'
  )

  getDocumentSection(ix: number) {
    // Note: indexes might change if the template used in the test changes
    return this.#documentSection.nth(ix)
  }

  async assertDocumentVisible() {
    await this.#documentSection.first().waitUntilVisible()
  }

  get authorsSection(): AuthorsSection {
    return new AuthorsSection(this.getDocumentSection(0))
  }

  get followupQuestionCount(): Promise<number> {
    return this.#followupQuestions.count()
  }
}

export class VasuEditPage extends VasuPageCommon {
  readonly modalOkButton = this.page.find('[data-qa="modal-okBtn"]')

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

  readonly #vasuPreviewBtn = this.page.find('[data-qa="vasu-preview-btn"]')

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

  get previewBtn(): Element {
    return this.#vasuPreviewBtn
  }

  async waitUntilSaved(): Promise<void> {
    await waitUntilFalse(() => this.previewBtn.disabled)
  }
}

export class VasuPage extends VasuPageCommon {
  readonly finalizeButton = this.page.find(
    '[data-qa="transition-button-MOVED_TO_READY"]'
  )
  readonly modalOkButton = this.page.find('[data-qa="modal-okBtn"]')
}
