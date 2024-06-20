// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, TextInput, Element } from '../../../utils/page'

export class ChildDocumentPage {
  status: Element
  savingIndicator: Element
  previewButton: Element
  editButton: Element
  returnButton: Element
  constructor(private readonly page: Page) {
    this.status = page.findByDataQa('document-state-chip')
    this.savingIndicator = page.findByDataQa('saving-spinner')
    this.previewButton = page.findByDataQa('preview-button')
    this.editButton = page.findByDataQa('edit-button')
    this.returnButton = page.findByDataQa('return-button')
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
