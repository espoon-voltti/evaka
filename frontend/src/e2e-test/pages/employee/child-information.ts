// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import config from '../../config'
import { Daycare } from '../../dev-api/types'
import { waitUntilEqual, waitUntilTrue } from '../../utils'
import {
  Checkbox,
  Combobox,
  DatePickerDeprecated,
  Element,
  FileInput,
  Modal,
  Page,
  Radio,
  Select,
  TextInput
} from '../../utils/page'

import CreateApplicationModal from './applications/create-application-modal'
import { IncomeSection } from './guardian-information'

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

  async navigateToChild(id: UUID) {
    await this.page.goto(config.employeeUrl + '/child-information/' + id)
  }

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

  async assertCollapsiblesVisible(params: Record<Collapsible, boolean>) {
    const collapsibleArray = Object.entries(collapsibles) as [
      Collapsible,
      { selector: string }
    ][]

    await waitUntilEqual(async () => {
      const collapsibleStatus = collapsibleArray.map(
        async ([key, { selector }]) => {
          const isVisible = await this.page.find(selector).visible
          return [key, isVisible] as const
        }
      )
      return Object.fromEntries(await Promise.all(collapsibleStatus))
    }, params)
  }

  async openCollapsible<C extends Collapsible>(
    collapsible: C
  ): Promise<SectionFor<C>> {
    const { selector, section } = collapsibles[collapsible]
    const element = this.page.find(selector)
    await element.click()
    return new section(this.page, element) as SectionFor<C>
  }

  additionalInformationSection() {
    return new AdditionalInformationSection(
      this.page,
      this.page.find('[data-qa="additional-information-section"]')
    )
  }
}

class Section extends Element {
  constructor(protected page: Page, root: Element) {
    super(root)
  }
}

export class AdditionalInformationSection extends Section {
  #medication = this.find('[data-qa="medication"]')
  #editBtn = this.find('[data-qa="edit-child-settings-button"]')
  #medicationInput = new TextInput(this.find('[data-qa="medication-input"]'))
  #confirmBtn = this.find('[data-qa="confirm-edited-child-button"]')

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

export class DailyServiceTimeSection extends Section {
  readonly #typeText = this.find('[data-qa="times-type"]')
  readonly #timesText = this.find('[data-qa="times"]')
  readonly #editButton = this.find('[data-qa="edit-button"]')

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
    return new DailyServiceTimesSectionEdit(this.page, this)
  }
}

export class DailyServiceTimesSectionEdit extends Section {
  readonly #notSetRadio = this.find('[data-qa="radio-not-set"]')

  readonly #regularRadio = new Radio(this.find('[data-qa="radio-regular"]'))
  readonly #irregularRadio = new Radio(this.find('[data-qa="radio-irregular"]'))
  readonly #submitButton = this.find('[data-qa="submit-button"]')

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
    return new TextInput(this.find(`[data-qa="${which}-${startEnd}"]`))
  }

  async fillTimeRange(which: string, start: string, end: string) {
    await this.#timeInput(which, 'start').fill(start)
    await this.#timeInput(which, 'end').fill(end)
  }

  #checkbox(day: string) {
    return this.find(`[data-qa="${day}-checkbox"]`)
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

export class PedagogicalDocumentsSection extends Section {
  readonly #startDate = this.find('[data-qa="pedagogical-document-start-date"]')

  readonly #document = this.find('[data-qa="pedagogical-document-document"]')

  readonly #descriptionInput = new TextInput(
    this.find('[data-qa="pedagogical-document-description"]')
  )

  readonly #description = this.find(
    '[data-qa="pedagogical-document-description"]'
  )

  readonly #create = this.find('[data-qa="button-create-pedagogical-document"]')
  readonly #save = this.find('[data-qa="pedagogical-document-button-save"]')
  readonly #edit = this.find('[data-qa="pedagogical-document-button-edit"]')
  readonly #cancel = this.find('[data-qa="pedagogical-document-button-cancel"]')
  readonly #delete = this.find('[data-qa="pedagogical-document-button-delete"]')

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
    return new PedagogicalDocumentsSection(this.page, this)
  }

  async addAttachmentAndAssert(
    page: Page,
    testfileName: string,
    testfilePath: string
  ) {
    await new FileInput(page.find('[data-qa="btn-upload-file"]')).setInputFiles(
      testfilePath
    )
    await waitUntilTrue(async () =>
      (
        await page.findAll('[data-qa="file-download-button"]').allInnerTexts()
      ).includes(testfileName)
    )
  }
}

export class VasuAndLeopsSection extends Section {
  readonly #addNew = this.find('[data-qa="add-new-vasu-button"]')

  async addNew() {
    return this.#addNew.click()
  }
}

export class BackupCaresSection extends Section {
  #createBackupCareButton = this.find('[data-qa="backup-care-create-btn"]')
  #backupCareSelectUnit = new Combobox(
    this.find('[data-qa="backup-care-select-unit"]')
  )

  #dates = this.findAll('[data-qa="dates"] > *')
  #startDate = new DatePickerDeprecated(this.#dates.nth(0))
  #endDate = new DatePickerDeprecated(this.#dates.nth(1))

  // #backupCareForm = this.find('[data-qa="backup-care-form"]')
  #submit = this.find('[data-qa="submit-backup-care-form"]')

  #backupCares = this.find('[data-qa="backup-cares"]')

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
    return this.evaluate((el) => {
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

export class FamilyContactsSection extends Section {
  #row(name: string) {
    return this.find(`[data-qa="table-backup-pickup-row-${name}"]`)
  }

  #createBtn = this.find('[data-qa="create-backup-pickup-btn"]')
  #nameInput = new TextInput(this.find('[data-qa="backup-pickup-name-input"]'))
  #phoneInput = new TextInput(
    this.find('[data-qa="backup-pickup-phone-input"]')
  )
  #modalOk = this.find('[data-qa="modal-okBtn"]')

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

export class GuardiansSection extends Section {
  #guardianRows = this.find('[data-qa="table-guardian-row"]')

  async assertGuardianExists(ssn: string) {
    await this.#guardianRows
      .find(`[data-qa="guardian-ssn"] >> text=${ssn}`)
      .waitUntilVisible()
  }
}

export class PlacementsSection extends Section {
  #placementRow = (id: string) => this.find(`[data-qa="placement-${id}"]`)
  #serviceNeedRow = (index: number) =>
    this.findAll('[data-qa="service-need-row"]').nth(index)
  #serviceNeedRowOptionName = (index: number) =>
    this.#serviceNeedRow(index).find('[data-qa="service-need-name"]')
  #addMissingServiceNeedButton = this.find(
    '[data-qa="add-new-missing-service-need"]'
  )
  #serviceNeedOptionSelect = new Select(
    this.find('[data-qa="service-need-option-select"]')
  )
  #serviceNeedSaveButton = this.find('[data-qa="service-need-save"]')
  #terminatedByGuardian = (placementId: string) =>
    this.#placementRow(placementId).find('[data-qa="placement-terminated"]')

  async openPlacement(id: string) {
    const placementRow = this.#placementRow(id)
    if ((await placementRow.getAttribute('data-status')) === 'closed') {
      await placementRow.find('[data-qa="collapsible-trigger"]').click()
    }
  }

  async addMissingServiceNeed(placementId: string, optionName: string) {
    await this.openPlacement(placementId)
    await this.#addMissingServiceNeedButton.click()
    await this.#serviceNeedOptionSelect.selectOption({ label: optionName })
    await this.#serviceNeedSaveButton.click()
  }

  async assertNthServiceNeedName(index: number, optionName: string) {
    await waitUntilEqual(
      () => this.#serviceNeedRowOptionName(index).textContent,
      optionName
    )
  }

  async assertServiceNeedOptions(placementId: string, optionIds: string[]) {
    await this.openPlacement(placementId)
    await this.#addMissingServiceNeedButton.click()
    await waitUntilTrue(async () => {
      const selectableOptions = await this.#serviceNeedOptionSelect
        .findAll('option')
        .evaluateAll((elements) =>
          elements
            .map((e) => e.getAttribute('value'))
            .filter((value): value is string => !!value)
        )

      return (
        optionIds.every((optionId) => selectableOptions.includes(optionId)) &&
        selectableOptions.every((optionId) => optionIds.includes(optionId))
      )
    })
  }

  async assertTerminatedByGuardianIsShown(placementId: string) {
    await this.#terminatedByGuardian(placementId).waitUntilVisible()
  }

  async assertTerminatedByGuardianIsNotShown(placementId: string) {
    await this.#placementRow(placementId)
      .find('[data-qa="placement-details-start-date"]')
      .waitUntilVisible()
    await this.#terminatedByGuardian(placementId).waitUntilHidden()
  }

  async createNewPlacement({
    unitName,
    startDate,
    endDate
  }: {
    unitName: string
    startDate: string
    endDate: string
  }) {
    await this.find('[data-qa="create-new-placement-button"]').click()

    const modal = new Modal(this.page.find('[data-qa="modal"]'))
    const unitSelect = new Combobox(modal.find('[data-qa="unit-select"]'))
    await unitSelect.fillAndSelectFirst(unitName)

    const start = new DatePickerDeprecated(
      modal.find('[data-qa="create-placement-start-date"]')
    )
    await start.fill(startDate)

    const end = new DatePickerDeprecated(
      modal.find('[data-qa="create-placement-end-date"]')
    )
    await end.fill(endDate)

    await modal.submit()
  }
}

export class AssistanceNeedSection extends Section {
  #createAssistanceNeedButton = this.find(
    '[data-qa="assistance-need-create-btn"]'
  )
  #assistanceNeedMultiplierInput = new TextInput(
    this.find('[data-qa="input-assistance-need-multiplier"]')
  )
  #confirmAssistanceNeedButton = this.find(
    '[data-qa="button-assistance-need-confirm"]'
  )
  #assistanceNeedMultiplier = this.findAll(
    '[data-qa="assistance-need-multiplier"]'
  )
  #assistanceNeedRow = this.findAll('[data-qa="assistance-need-row"]')

  async createNewAssistanceNeed() {
    await this.#createAssistanceNeedButton.click()
  }

  async setAssistanceNeedMultiplier(multiplier: string) {
    await this.#assistanceNeedMultiplierInput.fill(multiplier)
  }

  async confirmAssistanceNeed() {
    await this.#confirmAssistanceNeedButton.click()
  }

  async assertAssistanceNeedMultiplier(expected: string, nth = 0) {
    await waitUntilEqual(
      () => this.#assistanceNeedMultiplier.nth(nth).innerText,
      expected
    )
  }

  async assertAssistanceNeedCount(expectedCount: number) {
    await waitUntilEqual(() => this.#assistanceNeedRow.count(), expectedCount)
  }
}

class MessageBlocklistSection extends Section {
  async addParentToBlockList(parentId: string) {
    const checkbox = new Checkbox(
      this.find(
        `[data-qa="recipient-${parentId}"] [data-qa="blocklist-checkbox"]`
      )
    )

    // This causes a request to backend
    await checkbox.click()

    // It gets unchecked when the request is finished
    await checkbox.waitUntilChecked(false)
  }
}

class FeeAlterationsSection extends Section {}

class ApplicationsSection extends Section {
  #createApplication = this.find('[data-qa="button-create-application"]')

  async openCreateApplicationModal() {
    await this.#createApplication.click()
    return new CreateApplicationModal(
      this.page,
      this.page.find('[data-qa="modal"]')
    )
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
  },
  placements: {
    selector: '[data-qa="child-placements-collapsible"]',
    section: PlacementsSection
  },
  assistanceNeed: {
    selector: '[data-qa="assistance-collapsible"]',
    section: AssistanceNeedSection
  },
  messageBlocklist: {
    selector: '[data-qa="child-message-blocklist-collapsible"]',
    section: MessageBlocklistSection
  },
  applications: {
    selector: '[data-qa="applications-collapsible"]',
    section: ApplicationsSection
  },
  feeAlterations: {
    selector: '[data-qa="fee-alteration-collapsible"]',
    section: FeeAlterationsSection
  },
  income: {
    selector: '[data-qa="income-collapsible"]',
    section: IncomeSection
  }
}

type Collapsibles = typeof collapsibles
type Collapsible = keyof Collapsibles
type SectionFor<C extends Collapsible> = InstanceType<
  Collapsibles[C]['section']
>
