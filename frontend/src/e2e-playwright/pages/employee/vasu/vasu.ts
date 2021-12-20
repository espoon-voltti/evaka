// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilFalse } from 'e2e-playwright/utils'
import { Page, TextInput, Element } from '../../../utils/page'
import {
  AuthorsSection,
  ConsiderationsSection,
  GoalsSection,
  PreviousVasuGoalsSection
} from './pageSections'

class VasuPageCommon {
  constructor(readonly page: Page) {}

  readonly #documentSection = this.page.findAll(
    '[data-qa="vasu-document-section"]'
  )
  readonly #followupQuestions = this.page.findAll(
    '[data-qa="vasu-followup-question"]'
  )

  getDocumentSection(ix: number): Element {
    // Note: indexes might change if the template used in the test changes
    return this.#documentSection.nth(ix)
  }

  async assertDocumentVisible() {
    await this.#documentSection.first().waitUntilVisible()
  }

  get authorsSection(): AuthorsSection {
    return new AuthorsSection(this.getDocumentSection(0))
  }

  get considerationsSection(): ConsiderationsSection {
    return new ConsiderationsSection(this.getDocumentSection(1))
  }

  get previousVasuGoalsSection(): PreviousVasuGoalsSection {
    return new PreviousVasuGoalsSection(this.getDocumentSection(2))
  }

  get goalsSection(): GoalsSection {
    return new GoalsSection(this.getDocumentSection(3))
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
