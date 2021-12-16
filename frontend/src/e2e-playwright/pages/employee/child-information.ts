// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Page,
  Element,
  FileInput,
  TextInput,
  Checkbox,
  Radio,
  Combobox,
  DatePickerDeprecated
} from 'e2e-playwright/utils/page'
import { waitUntilEqual, waitUntilTrue } from '../../utils'
import { Daycare } from '../../../e2e-test-common/dev-api/types'

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

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="person-details-section"][data-isloading="false"]')
      .waitUntilVisible()
    await this.page
      .find(
        '[data-qa="additional-information-section"][data-isloading="false"]'
      )
      .waitUntilVisible()
  }

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

  additionalInformationSection() {
    return new AdditionalInformationSection(
      this.page.find('[data-qa="additional-information-section"]')
    )
  }
}

export class AdditionalInformationSection {
  constructor(private section: Element) {}

  #medication = this.section.find('[data-qa="medication"]')
  #editBtn = this.section.find('[data-qa="edit-child-settings-button"]')
  #medicationInput = new TextInput(
    this.section.find('[data-qa="medication-input"]')
  )
  #confirmBtn = this.section.find('[data-qa="confirm-edited-child-button"]')

  async assertMedication(text: string) {
    await waitUntilEqual(() => this.#medication.textContent, text)
  }

  async fillMedication(text: string) {
    await this.#medicationInput.fill(text)
  }

  async edit() {
    await this.#editBtn.click()
  }

  async save() {
    await this.#confirmBtn.click()
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

  readonly #regularRadio = new Radio(
    this.section.find('[data-qa="radio-regular"]')
  )
  readonly #irregularRadio = new Radio(
    this.section.find('[data-qa="radio-irregular"]')
  )
  readonly #submitButton = this.section.find('[data-qa="submit-button"]')

  constructor(private section: Element) {}

  async selectTimeNotSet() {
    await this.#notSetRadio.click()
  }

  async selectRegularTime() {
    await this.#regularRadio.click()
  }

  async regularTimeIsSelected() {
    return await this.#regularRadio.checked
  }

  async selectIrregularTime() {
    await this.#irregularRadio.click()
  }

  #timeInput(which: string, startEnd: 'start' | 'end') {
    return new TextInput(this.section.find(`[data-qa="${which}-${startEnd}"]`))
  }

  async fillTimeRange(which: string, start: string, end: string) {
    await this.#timeInput(which, 'start').fill(start)
    await this.#timeInput(which, 'end').fill(end)
  }

  #checkbox(day: string) {
    return this.section.find(`[data-qa="${day}-checkbox"]`)
  }

  async selectDay(day: string) {
    await this.#checkbox(day).click()
  }

  async dayIsSelected(day: string) {
    return new Checkbox(this.#checkbox(day)).checked
  }

  async hasTimeRange(day: string, start: string, end: string) {
    return (
      (await this.#timeInput(day, 'start').inputValue) === start &&
      (await this.#timeInput(day, 'end').inputValue) === end
    )
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

export class BackupCaresSection {
  constructor(private section: Element) {}

  #createBackupCareButton = this.section.find(
    '[data-qa="backup-care-create-btn"]'
  )
  #backupCareSelectUnit = new Combobox(
    this.section.find('[data-qa="backup-care-select-unit"]')
  )

  #dates = this.section.findAll('[data-qa="dates"] > *')
  #startDate = new DatePickerDeprecated(this.#dates.nth(0))
  #endDate = new DatePickerDeprecated(this.#dates.nth(1))

  // #backupCareForm = this.section.find('[data-qa="backup-care-form"]')
  #submit = this.section.find('[data-qa="submit-backup-care-form"]')

  #backupCares = this.section.find('[data-qa="backup-cares"]')

  async createBackupCare(daycare: Daycare, startDate: string, endDate: string) {
    await this.#createBackupCareButton.click()
    await this.#backupCareSelectUnit.fillAndSelectFirst(daycare.name)

    await this.#startDate.fill(startDate)
    await this.#endDate.fill(endDate)

    await this.#submit.click()
    await this.#backupCares.waitUntilVisible()
  }

  async getBackupCares(): Promise<Array<{ unit: string; period: string }>> {
    await this.#backupCares.waitUntilVisible()
    return this.section.evaluate((el) => {
      return Array.from(el.querySelectorAll('[data-qa="backup-care-row"]')).map(
        (row) => ({
          unit: row.querySelector('[data-qa="unit"]')?.textContent ?? '',
          period: row.querySelector('[data-qa="period"]')?.textContent ?? ''
        })
      )
    })
  }

  async deleteBackupCare(index: number) {
    await this.#backupCares
      .findAll('[data-qa="backup-care-row"]')
      .nth(index)
      .find('[data-qa="btn-remove-backup-care"]')
      .click()

    const modal = this.#backupCares.find('[data-qa="modal"]')
    await modal.waitUntilVisible()
    await this.#backupCares.find('[data-qa="modal-okBtn"]').click()
    await modal.waitUntilHidden()
  }
}

export class FamilyContactsSection {
  constructor(private section: Element) {}

  #row(name: string) {
    return this.section.find(`[data-qa="table-backup-pickup-row-${name}"]`)
  }

  #createBtn = this.section.find('[data-qa="create-backup-pickup-btn"]')
  #nameInput = new TextInput(
    this.section.find('[data-qa="backup-pickup-name-input"]')
  )
  #phoneInput = new TextInput(
    this.section.find('[data-qa="backup-pickup-phone-input"]')
  )
  #modalOk = this.section.find('[data-qa="modal-okBtn"]')

  async addBackupPickup(name: string, phone: string) {
    await this.#createBtn.click()
    await this.#nameInput.fill(name)
    await this.#phoneInput.type(phone)
    await this.#modalOk.click()
  }

  async assertBackupPickupExists(name: string) {
    await this.#row(name).waitUntilVisible()
  }

  async assertBackupPickupDoesNotExist(name: string) {
    await this.#row(name).waitUntilHidden()
  }

  async deleteBackupPickup(name: string) {
    await this.#row(name).find('[data-qa="delete-backup-pickup"]').click()
    await this.#modalOk.click()
  }
}

export class GuardiansSection {
  constructor(private section: Element) {}

  #guardianRows = this.section.find('[data-qa="table-guardian-row"]')

  async assertGuardianExists(ssn: string) {
    await this.#guardianRows
      .find(`[data-qa="guardian-ssn"] >> text=${ssn}`)
      .waitUntilVisible()
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
  },
  backupCares: {
    selector: '[data-qa="backup-cares-collapsible"]',
    section: BackupCaresSection
  },
  familyContacts: {
    selector: '[data-qa="family-contacts-collapsible"]',
    section: FamilyContactsSection
  },
  guardians: {
    selector: '[data-qa="person-guardians-collapsible"]',
    section: GuardiansSection
  }
}

type Collapsibles = typeof collapsibles
type Collapsible = keyof Collapsibles
type SectionFor<C extends Collapsible> = InstanceType<
  Collapsibles[C]['section']
>
