// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type DateRange from 'lib-common/date-range'

import type { DevEmployee } from '../../../generated/api-types'
import type { Page, Element } from '../../../utils/page'
import {
  TextInput,
  AsyncButton,
  Combobox,
  DatePicker,
  Modal,
  Checkbox
} from '../../../utils/page'

export class ChildDocumentPage {
  status: Element
  savingIndicator: Element
  previewButton: Element
  editButton: Element
  returnButton: Element
  sendButton: Element
  archiveButton: AsyncButton
  archiveTooltip: Element
  acceptDecisionButton: Element
  sendingConfirmationModal: Modal
  confirmOtherDecisionsButton: Element
  documentSection: Element

  constructor(private readonly page: Page) {
    this.status = page.findByDataQa('document-state-chip')
    this.savingIndicator = page.findByDataQa('saving-spinner')
    this.previewButton = page.findByDataQa('preview-button')
    this.editButton = page.findByDataQa('edit-button')
    this.returnButton = page.findByDataQa('return-button')
    this.sendButton = new AsyncButton(page.findByDataQa('send-button'))
    this.archiveButton = new AsyncButton(page.findByDataQa('archive-button'))
    this.archiveTooltip = page.findByDataQa('archive-tooltip')
    this.acceptDecisionButton = page.findByDataQa('accept-decision-button')
    this.sendingConfirmationModal = new Modal(page.findByDataQa('modal'))
    this.confirmOtherDecisionsButton = page.findByDataQa(
      'confirm-other-decisions-button'
    )
    this.documentSection = page.findByDataQa('document-section')
  }

  getTextQuestion(sectionName: string, questionName: string) {
    const section = this.page.find('[data-qa="document-section"]', {
      hasText: sectionName
    })
    const question = section.find('[data-qa="document-question"]', {
      hasText: questionName
    })
    return new TextInput(question.findByDataQa('answer-input'))
  }

  getTextAnswer(sectionName: string, questionName: string) {
    const section = this.page.find('[data-qa="document-section"]', {
      hasText: sectionName
    })
    const question = section.find('[data-qa="document-question-preview"]', {
      hasText: questionName
    })
    return question.findByDataQa('answer-preview')
  }

  getCheckboxGroupAnswer(sectionName: string, questionName: string) {
    const section = this.page.find('[data-qa="document-section"]', {
      hasText: sectionName
    })
    const question = section.find('[data-qa="document-question-preview"]', {
      hasText: questionName
    })
    return question.findByDataQa('answer-preview')
  }

  async publish() {
    await this.page.findByDataQa('publish-button').click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async goToNextStatus() {
    await this.page.findByDataQa('next-status-button').click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async goToPrevStatus() {
    await this.page.findByDataQa('prev-status-button').click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async goToCompletedStatus() {
    await this.page.findByDataQa('next-status-button').click()
    await new Checkbox(
      this.page.findByDataQa('modal-extra-confirmation')
    ).check()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async proposeDecision(decisionMaker: DevEmployee) {
    const decisionMakerSelect = new Combobox(
      this.page.findByDataQa('decision-maker-combobox')
    )
    await decisionMakerSelect.fillAndSelectItem(
      decisionMaker.firstName,
      decisionMaker.id
    )
    await this.page.findByDataQa('propose-decision-button').click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async acceptDecision(validity: DateRange) {
    await this.acceptDecisionButton.click()
    const validityStartPicker = new DatePicker(
      this.page
        .findByDataQa('decision-validity-picker')
        .findByDataQa('start-date')
    )
    await validityStartPicker.fill(validity.start)
    if (validity.end) {
      const validityEndPicker = new DatePicker(
        this.page
          .findByDataQa('decision-validity-picker')
          .findByDataQa('end-date')
      )
      await validityEndPicker.fill(validity.end)
    }
    await this.page.findByDataQa('accept-decision-confirm-button').click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async rejectDecision() {
    await this.page.findByDataQa('reject-decision-button').click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async annulDecision() {
    await this.page.findByDataQa('annul-decision-button').click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async closeConcurrentEditErrorModal() {
    await this.page
      .findByDataQa('concurrent-edit-error-modal')
      .findByDataQa('modal-okBtn')
      .click()
  }

  async selectOneOptionForAllOtherDecisions(option: boolean) {
    const otherDecisions = this.page.findAllByDataQa('other-decision')
    const otherDecisionsCount = await otherDecisions.count()

    if (otherDecisionsCount > 0) {
      for (let i = 0; i < otherDecisionsCount; i++) {
        const decision = otherDecisions.nth(i)
        if (option) {
          await decision.findByDataQa('other-decision-option-0').click()
        } else {
          await decision.findByDataQa('other-decision-option-1').click()
        }
      }
    }
  }

  async selectDifferentOptionsForOtherDecisions() {
    const otherDecisions = this.page.findAllByDataQa('other-decision')
    const otherDecisionsCount = await otherDecisions.count()

    if (otherDecisionsCount > 0) {
      for (let i = 0; i < otherDecisionsCount; i++) {
        const decision = otherDecisions.nth(i)
        if (i % 2 === 0) {
          await decision.findByDataQa('other-decision-option-0').click()
        } else {
          await decision.findByDataQa('other-decision-option-1').click()
        }
      }
    }
  }

  async clickModalOkButton() {
    await this.page.findByDataQa('modal-okBtn').click()
  }
}
