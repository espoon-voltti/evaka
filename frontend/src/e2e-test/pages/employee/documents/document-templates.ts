// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Locator } from 'playwright'

import {
  Checkbox,
  Element,
  FileInput,
  MultiSelect,
  Page,
  Select,
  TextInput
} from '../../../utils/page'

export class DocumentTemplatesListPage {
  createNewButton: Element
  importTemplate: Element
  templateImportModal: TemplateImportModal
  templateModal: TemplateModal
  constructor(private readonly page: Page) {
    this.createNewButton = page.findByDataQa('create-template-button')
    this.importTemplate = page.findByDataQa('import-template')
    this.templateImportModal = new TemplateImportModal(
      page.findByDataQa('template-import-modal').locator
    )
    this.templateModal = new TemplateModal(
      page.findByDataQa('template-modal').locator
    )
  }

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
  readonly placementTypesSelect
  readonly validityStartInput
  readonly confirmCreateButton

  constructor(locator: Locator) {
    super(locator)
    this.nameInput = new TextInput(this.findByDataQa('name-input'))
    this.typeSelect = new Select(this.findByDataQa('type-select'))
    this.placementTypesSelect = new MultiSelect(
      this.findByDataQa('placement-types-select')
    )
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
  createNewSectionButton: Element
  sectionNameInput: TextInput
  confirmCreateSectionButton: Element
  questionLabelInput: TextInput
  confirmCreateQuestionButton: Element
  publishCheckbox: Checkbox
  saveButton: Element
  constructor(private readonly page: Page) {
    this.createNewSectionButton = page.findByDataQa('create-section-button')
    this.sectionNameInput = new TextInput(page.findByDataQa('name-input'))
    this.confirmCreateSectionButton = page.findByDataQa('modal-okBtn')
    this.questionLabelInput = new TextInput(
      page.findByDataQa('question-label-input')
    )
    this.confirmCreateQuestionButton = page.findByDataQa('modal-okBtn')
    this.publishCheckbox = new Checkbox(
      page.findByDataQa('ready-to-publish-checkbox')
    )
    this.saveButton = page.findByDataQa('save-template')
  }

  getSection(name: string) {
    return new Section(
      this.page.find('[data-qa="template-section"]', { hasText: name })
    )
  }
}

class Section {
  createNewQuestionButton: Element
  constructor(readonly element: Element) {
    this.createNewQuestionButton = element.findByDataQa(
      'create-question-button'
    )
  }
}
