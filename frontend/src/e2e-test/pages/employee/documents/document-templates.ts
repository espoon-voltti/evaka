// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Checkbox, Element, Page, Select, TextInput } from '../../../utils/page'

export class DocumentTemplatesListPage {
  constructor(private readonly page: Page) {}

  readonly createNewButton = this.page.findByDataQa('create-template-button')

  readonly nameInput = new TextInput(this.page.findByDataQa('name-input'))
  readonly typeSelect = new Select(this.page.findByDataQa('type-select'))
  readonly validityStartInput = new TextInput(
    this.page.findByDataQa('start-date')
  )
  readonly confirmCreateButton = this.page.findByDataQa('modal-okBtn')

  async openTemplate(name: string) {
    await this.page.find('a', { hasText: name }).click()
  }
}

export class DocumentTemplateEditorPage {
  constructor(private readonly page: Page) {}

  readonly createNewSectionButton = this.page.findByDataQa(
    'create-section-button'
  )
  readonly sectionNameInput = new TextInput(
    this.page.findByDataQa('name-input')
  )
  readonly confirmCreateSectionButton = this.page.findByDataQa('modal-okBtn')

  getSection(name: string) {
    return new Section(
      this.page.find('[data-qa="template-section"]', { hasText: name })
    )
  }

  readonly questionLabelInput = new TextInput(
    this.page.findByDataQa('question-label-input')
  )
  readonly confirmCreateQuestionButton = this.page.findByDataQa('modal-okBtn')

  readonly publishCheckbox = new Checkbox(
    this.page.findByDataQa('ready-to-publish-checkbox')
  )
  readonly saveButton = this.page.findByDataQa('save-template')
}

class Section {
  constructor(readonly element: Element) {}

  readonly createNewQuestionButton = this.element.findByDataQa(
    'create-question-button'
  )
}
