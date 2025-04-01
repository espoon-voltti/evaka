// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, TextInput, Element, AsyncButton } from '../../../utils/page'

export class ChildDocumentPage {
  status: Element
  savingIndicator: Element
  previewButton: Element
  editButton: Element
  returnButton: Element
  sendButton: Element
  archiveButton: AsyncButton
  archiveTooltip: Element
  constructor(private readonly page: Page) {
    this.status = page.findByDataQa('document-state-chip')
    this.savingIndicator = page.findByDataQa('saving-spinner')
    this.previewButton = page.findByDataQa('preview-button')
    this.editButton = page.findByDataQa('edit-button')
    this.returnButton = page.findByDataQa('return-button')
    this.sendButton = new AsyncButton(page.findByDataQa('send-button'))
    this.archiveButton = new AsyncButton(page.findByDataQa('archive-button'))
    this.archiveTooltip = page.findByDataQa('archive-tooltip')
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

  async publish() {
    await this.page.findByDataQa('publish-button').click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async goToNextStatus() {
    await this.page.findByDataQa('next-status-button').click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async closeConcurrentEditErrorModal() {
    await this.page
      .findByDataQa('concurrent-edit-error-modal')
      .findByDataQa('modal-okBtn')
      .click()
  }
}
