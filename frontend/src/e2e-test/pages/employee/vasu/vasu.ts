// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual, waitUntilTrue } from '../../../utils'
import { Page, TextInput, Element, Checkbox } from '../../../utils/page'

import {
  AuthoringSection,
  BasicInfoSection,
  CooperationSection,
  DiscussionSection,
  EvaluationSection,
  GoalsSection,
  InfoSharedToSection,
  OtherDocsAndPlansSection,
  VasuGoalsSection,
  OtherSection
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

  get basicInfoSection(): BasicInfoSection {
    return new BasicInfoSection(this.getDocumentSection(0))
  }

  get authoringSection(): AuthoringSection {
    return new AuthoringSection(this.getDocumentSection(1))
  }

  get cooperationSection(): CooperationSection {
    return new CooperationSection(this.getDocumentSection(2))
  }

  get vasuGoalsSection(): VasuGoalsSection {
    return new VasuGoalsSection(this.getDocumentSection(3))
  }

  get goalsSection(): GoalsSection {
    return new GoalsSection(this.getDocumentSection(4))
  }

  get otherSection(): OtherSection {
    return new OtherSection(this.getDocumentSection(5))
  }

  get otherDocsAndPlansSection(): OtherDocsAndPlansSection {
    return new OtherDocsAndPlansSection(this.getDocumentSection(6))
  }

  get infoSharedToSection(): InfoSharedToSection {
    return new InfoSharedToSection(this.getDocumentSection(7))
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

  #followup(nth: number) {
    const question = this.page
      .findAllByDataQa('vasu-followup-question')
      .nth(nth)
    return {
      entryInput: (nth: number) =>
        new TextInput(question.findByDataQa(`follow-up-${nth}-input`)),
      entryDateInput: (nth: number) =>
        new TextInput(question.findByDataQa(`follow-up-${nth}-date`)),
      meta: (nth: number) => question.findByDataQa(`follow-up-${nth}-meta`)
    }
  }

  #followupAddBtn = this.page
    .findByDataQa('vasu-followup-question')
    .findByDataQa('followup-add-btn')

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

  async addFollowup() {
    await this.#followupAddBtn.click()
  }

  async inputFollowupWithDateComment(
    comment: string,
    date: string,
    nth: number,
    entryNth: number
  ) {
    const dateInput = this.#followup(nth).entryDateInput(entryNth)
    await dateInput.clear()
    await dateInput.type(date)
    await waitUntilEqual(() => dateInput.inputValue, date)
    await this.inputFollowupComment(comment, nth, entryNth)
  }

  async inputFollowupComment(comment: string, nth: number, entryNth: number) {
    const input = this.#followup(nth).entryInput(entryNth)
    await input.clear()
    await input.type(comment)
    await waitUntilEqual(() => input.inputValue, comment)
  }

  followupEntryMetadata(nth: number, entryNth: number): Element {
    return this.#followup(nth).meta(entryNth)
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
  readonly documentState = this.page.find(
    '[data-qa="vasu-event-list"] [data-qa="vasu-state-chip"]'
  )

  readonly #templateName = this.page.find('[data-qa="template-name"]')
  readonly #backButton = this.page.findByDataQa('back-button')
  readonly #editButton = this.page.find('[data-qa="edit-button"]')

  // The (first) label for the state chip has no corresponding span, so the index is off by one.
  #valueForLabel = (label: string): Promise<string> =>
    this.#vasuEventListLabels
      .allTexts()
      .then((labels) =>
        labels.reduce(
          async (acc, l, ix) =>
            l === label
              ? await this.#vasuEventListValues.nth(ix - 1).text
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
    await this.#templateName.assertTextEquals(expected)
  }

  async back() {
    await this.#backButton.click()
  }

  async edit() {
    await this.#editButton.click()
  }

  async followupEntry(sectionNth: number, entryNth: number) {
    const entry = this.page
      .findAllByDataQa('vasu-followup-question')
      .nth(sectionNth)
      .findAllByDataQa('follow-up-entry')
      .nth(entryNth)

    return {
      date: await entry.findByDataQa('follow-up-entry-date').text,
      text: await entry.findByDataQa('follow-up-entry-text').text
    }
  }
}

export class VasuPreviewPage extends VasuPageCommon {
  readonly #multiselectAnswer = (questionNumber: string) =>
    this.page.find(`[data-qa="value-or-no-record-${questionNumber}"]`)

  readonly #titleChildName = this.page.findByDataQa('title-child-name')
  readonly #confirmCheckBox = new Checkbox(
    this.page.findByDataQa('confirm-checkbox')
  )
  readonly #confirmButton = this.page.findByDataQa('confirm-button')

  async assertTitleChildName(expectedName: string) {
    await this.#titleChildName.assertTextEquals(expectedName)
  }

  async assertGivePermissionToShareSectionIsVisible() {
    await this.#confirmButton.waitUntilVisible()
  }

  async assertGivePermissionToShareSectionIsNotVisible() {
    await this.#titleChildName.waitUntilVisible()
    await this.#confirmButton.waitUntilHidden()
  }

  async givePermissionToShare() {
    await this.#confirmCheckBox.check()
    await this.#confirmButton.click()
  }

  async assertMultiSelectContains(
    questionNumber: string,
    expectedText: string
  ) {
    await waitUntilTrue(async () =>
      (await this.#multiselectAnswer(questionNumber).text).includes(
        expectedText
      )
    )
  }
}
