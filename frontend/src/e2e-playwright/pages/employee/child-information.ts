// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Element, FileInput, TextInput } from 'e2e-playwright/utils/page'
import { waitUntilEqual, waitUntilTrue } from '../../utils'

export default class ChildInformationPage {
  constructor(private readonly page: Page) {}

  readonly #deceased = this.page.find('[data-qa="deceased-label"]')
  readonly #ophPersonOidInput = new TextInput(
    this.page.find('[data-qa="person-oph-person-oid"]')
  )

  readonly #editButton = this.page.find(
    '[data-qa="edit-person-settings-button"]'
  )

  readonly #confirmButton = this.page.find(
    '[data-qa="confirm-edited-person-button"]'
  )

  async clickEdit() {
    await this.#editButton.click()
  }

  async deceasedIconIsShown() {
    await this.#deceased.waitUntilVisible()
  }

  async assertOphPersonOid(expected: string) {
    await waitUntilEqual(() => this.#ophPersonOidInput.innerText, expected)
  }

  async setOphPersonOid(text: string) {
    await this.#ophPersonOidInput.click()
    await this.#ophPersonOidInput.type(text)
    await this.#confirmButton.click()
  }

  async openCollapsible<C extends Collapsible>(
    collapsible: C
  ): Promise<SectionFor<C>> {
    const { selector, section } = collapsibles[collapsible]
    const element = this.page.find(selector)
    await element.click()
    return new section(element) as SectionFor<C>
  }
}

export class DailyServiceTimeSection {
  constructor(private section: Element) {}

  readonly #typeText = this.section.find('[data-qa="times-type"]')
  readonly #timesText = this.section.find('[data-qa="times"]')
  readonly #editButton = this.section.find('[data-qa="edit-button"]')

  get typeText(): Promise<string> {
    return this.#typeText.innerText
  }

  get timesText(): Promise<string> {
    return this.#timesText.innerText
  }

  get hasTimesText(): Promise<boolean> {
    return this.#timesText.visible
  }

  async edit() {
    await this.#editButton.click()
    return new DailyServiceTimesSectionEdit(this.section)
  }
}

export class DailyServiceTimesSectionEdit {
  readonly #notSetRadio = this.section.find('[data-qa="radio-not-set"]')

  readonly #regularRadio = this.section.find('[data-qa="radio-regular"]')
  readonly #irregularRadio = this.section.find('[data-qa="radio-irregular"]')
  readonly #submitButton = this.section.find('[data-qa="submit-button"]')

  constructor(private section: Element) {}

  async selectTimeNotSet() {
    await this.#notSetRadio.click()
  }

  async selectRegularTime() {
    await this.#regularRadio.click()
  }

  async selectIrregularTime() {
    await this.#irregularRadio.click()
  }

  async fillTimeRange(which: string, start: string, end: string) {
    await new TextInput(this.section.find(`[data-qa="${which}-start"]`)).fill(
      start
    )
    await new TextInput(this.section.find(`[data-qa="${which}-end"]`)).fill(end)
  }

  async selectDay(day: string) {
    await this.section.find(`[data-qa="${day}-checkbox"]`).click()
  }

  get submitIsDisabled(): Promise<boolean> {
    return this.#submitButton.disabled
  }

  async submit() {
    await this.#submitButton.click()
  }
}

export class PedagogicalDocumentsSection {
  constructor(private section: Element) {}

  readonly testFileName = 'test_file.png'
  testFilePath = `src/e2e-playwright/assets/${this.testFileName}`

  readonly #startDate = this.section.find(
    '[data-qa="pedagogical-document-start-date"]'
  )

  readonly #document = this.section.find(
    '[data-qa="pedagogical-document-document"]'
  )

  readonly #descriptionInput = new TextInput(
    this.section.find('[data-qa="pedagogical-document-description"]')
  )

  readonly #description = this.section.find(
    '[data-qa="pedagogical-document-description"]'
  )

  readonly #create = this.section.find(
    '[data-qa="button-create-pedagogical-document"]'
  )
  readonly #save = this.section.find(
    '[data-qa="pedagogical-document-button-save"]'
  )
  readonly #edit = this.section.find(
    '[data-qa="pedagogical-document-button-edit"]'
  )
  readonly #cancel = this.section.find(
    '[data-qa="pedagogical-document-button-cancel"]'
  )
  readonly #delete = this.section.find(
    '[data-qa="pedagogical-document-button-delete"]'
  )

  async save() {
    await this.#save.click()
  }

  async edit() {
    await this.#edit.click()
  }

  async cancel() {
    await this.#cancel.click()
  }

  async delete() {
    await this.#delete.click()
  }

  get startDate(): Promise<string> {
    return this.#startDate.innerText
  }

  get document(): Promise<string> {
    return this.#document.innerText
  }

  get description(): Promise<string> {
    return this.#description.innerText
  }

  async setDescription(text: string) {
    await this.#description.click()
    await this.#descriptionInput.type(text)
  }

  async addNew() {
    await this.#create.click()
    return new PedagogicalDocumentsSection(this.section)
  }

  async addAttachment(page: Page) {
    await new FileInput(page.find('[data-qa="btn-upload-file"]')).setInputFiles(
      this.testFilePath
    )
    await waitUntilTrue(async () =>
      (
        await page.find('[data-qa="file-download-button"]').innerText
      ).includes(this.testFileName)
    )
  }
}

export class VasuAndLeopsSection {
  constructor(private section: Element) {}

  readonly #addNew = this.section.find('[data-qa="add-new-vasu-button"]')

  async addNew() {
    return this.#addNew.click()
  }
}

const collapsibles = {
  dailyServiceTimes: {
    selector: '[data-qa="child-daily-service-times-collapsible"]',
    section: DailyServiceTimeSection
  },
  pedagogicalDocuments: {
    selector: '[data-qa="pedagogical-documents-collapsible"]',
    section: PedagogicalDocumentsSection
  },
  vasuAndLeops: {
    selector: '[data-qa="vasu-and-leops-collapsible"]',
    section: VasuAndLeopsSection
  }
}

type Collapsibles = typeof collapsibles
type Collapsible = keyof Collapsibles
type SectionFor<C extends Collapsible> = InstanceType<
  Collapsibles[C]['section']
>
