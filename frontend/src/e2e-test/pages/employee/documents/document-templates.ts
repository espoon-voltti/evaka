// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Locator } from 'playwright'

import {
  Checkbox,
  Element,
  FileInput,
  Page,
  Select,
  TextInput
} from '../../../utils/page'

export class DocumentTemplatesListPage {
  constructor(private readonly page: Page) {}

  readonly createNewButton = this.page.findByDataQa('create-template-button')
  readonly importTemplate = this.page.findByDataQa('import-template')
  readonly templateImportModal = new TemplateImportModal(
    this.page.findByDataQa('template-import-modal').locator
  )
  readonly templateModal = new TemplateModal(
    this.page.findByDataQa('template-modal').locator
  )

  async openTemplate(name: string) {
    await this.page.find('a', { hasText: name }).click()
  }

  async openCreateModal(): Promise<TemplateModal> {
    await this.createNewButton.click()
    await this.templateModal.waitUntilVisible()
    return this.templateModal
  }

  async openImportModal(): Promise<TemplateImportModal> {
    await this.importTemplate.click()
    await this.templateImportModal.waitUntilVisible()
    return this.templateImportModal
  }

  templateRow(name: string): DocumentTemplateRow {
    const nameElement = this.page.page.locator('[data-qa="name"]', {
      hasText: name
    })
    return new DocumentTemplateRow(
      this.page.page.locator('[data-qa="template-row"]', {
        has: nameElement
      })
    )
  }
}

export class DocumentTemplateRow extends Element {
  readonly exportButton: Element
  constructor(locator: Locator) {
    super(locator)
    this.exportButton = this.findByDataQa('export')
  }

  async exportToPath(): Promise<string> {
    const downloadPromise = this.locator.page().waitForEvent('download')
    await this.exportButton.click()
    return await (await downloadPromise).path()
  }
}

export class TemplateModal extends Element {
  readonly nameInput
  readonly typeSelect
  readonly validityStartInput
  readonly confirmCreateButton

  constructor(locator: Locator) {
    super(locator)
    this.nameInput = new TextInput(this.findByDataQa('name-input'))
    this.typeSelect = new Select(this.findByDataQa('type-select'))
    this.validityStartInput = new TextInput(this.findByDataQa('start-date'))
    this.confirmCreateButton = this.findByDataQa('modal-okBtn')
  }
}

export class TemplateImportModal extends Element {
  readonly cancel: Element
  readonly file: FileInput
  readonly continue: Element
  constructor(locator: Locator) {
    super(locator)
    this.cancel = this.findByDataQa('cancel')
    this.file = new FileInput(this.find('input[type=file]'))
    this.continue = this.findByDataQa('continue')
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
