// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual, waitUntilTrue } from '../../../utils'
import { Page, TextInput, Element } from '../../../utils/page'
import {
  AdditionalInfoSection,
  AuthorsSection,
  ConsiderationsSection,
  DiscussionSection,
  EvaluationSection,
  GoalsSection,
  InfoSharedToSection,
  OtherDocsAndPlansSection,
  PreviousVasuGoalsSection,
  WellnessSupportSection
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

  get wellnessSupportSection(): WellnessSupportSection {
    return new WellnessSupportSection(this.getDocumentSection(4))
  }

  get otherDocsAndPlansSection(): OtherDocsAndPlansSection {
    return new OtherDocsAndPlansSection(this.getDocumentSection(5))
  }

  get infoSharedToSection(): InfoSharedToSection {
    return new InfoSharedToSection(this.getDocumentSection(6))
  }

  get additionalInfoSection(): AdditionalInfoSection {
    return new AdditionalInfoSection(this.getDocumentSection(7))
  }

  get discussionSection(): DiscussionSection {
    return new DiscussionSection(this.getDocumentSection(8))
  }

  get evaluationSection(): EvaluationSection {
    return new EvaluationSection(this.getDocumentSection(9))
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
  readonly #vasuContainer = this.page.find('[data-qa="vasu-container"]')

  readonly #multiSelectQuestionOption = (text: string) =>
    this.page.find(`[data-qa="multi-select-question-option-${text}"]`)
  readonly #multiSelectQuestionOptionTextInput = (text: string) =>
    new TextInput(
      this.page.find(
        `[data-qa="multi-select-question-option-text-input-${text}"]`
      )
    )

  async clickMultiSelectQuestionOption(key: string) {
    await this.#multiSelectQuestionOption(key).click()
  }

  async setMultiSelectQuestionOptionText(key: string, text: string) {
    await this.#multiSelectQuestionOptionTextInput(key).fill(text)
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

  get previewBtn(): Element {
    return this.#vasuPreviewBtn
  }

  async waitUntilSaved(): Promise<void> {
    await waitUntilEqual(
      () => this.#vasuContainer.getAttribute('data-status'),
      'clean'
    )
  }
}

export class VasuPage extends VasuPageCommon {
  readonly finalizeButton = this.page.find(
    '[data-qa="transition-button-MOVED_TO_READY"]'
  )
  readonly markReviewedButton = this.page.find(
    '[data-qa="transition-button-MOVED_TO_REVIEWED"]'
  )
  readonly markClosedButton = this.page.find(
    '[data-qa="transition-button-MOVED_TO_CLOSED"]'
  )
  readonly modalOkButton = this.page.find('[data-qa="modal-okBtn"]')
  readonly #vasuEventListLabels = this.page.findAll(
    '[data-qa="vasu-event-list"] label'
  )
  readonly #vasuEventListValues = this.page.findAll(
    '[data-qa="vasu-event-list"] span'
  )
  readonly #vasuEventListDocState = this.page.find(
    '[data-qa="vasu-event-list"] [data-qa="vasu-state-chip"]'
  )

  readonly #templateName = this.page.find('[data-qa="template-name"]')
  readonly #editButton = this.page.find('[data-qa="edit-button"]')

  documentState = () => this.#vasuEventListDocState.innerText

  // The (first) label for the state chip has no corresponding span, so the index is off by one.
  #valueForLabel = (label: string): Promise<string> =>
    this.#vasuEventListLabels
      .allInnerTexts()
      .then((labels) =>
        labels.reduce(
          async (acc, l, ix) =>
            l === label
              ? await this.#vasuEventListValues.nth(ix - 1).innerText
              : acc,
          Promise.resolve('')
        )
      )

  publishedToGuardiansDate = () =>
    this.#valueForLabel('Viimeksi julkaistu huoltajalle')
  publishedDate = () => this.#valueForLabel('Julkaistu Laadittu-tilaan')
  reviewedDate = () => this.#valueForLabel('Julkaistu Arvioitu-tilaan')
  closedDate = () => this.#valueForLabel('Päättynyt')

  async assertTemplateName(expected: string) {
    await waitUntilEqual(() => this.#templateName.textContent, expected)
  }

  async edit() {
    await this.#editButton.click()
  }
}

export class VasuPreviewPage extends VasuPageCommon {
  readonly #multiselectAnswer = (questionNumber: string) =>
    this.page.find(`[data-qa="value-or-no-record-${questionNumber}"]`)

  async assertMultiSelectContains(
    questionNumber: string,
    expectedText: string
  ) {
    await waitUntilTrue(async () =>
      (
        await this.#multiselectAnswer(questionNumber).innerText
      ).includes(expectedText)
    )
  }
}
