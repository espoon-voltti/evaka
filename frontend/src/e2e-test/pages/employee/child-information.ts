// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ShiftCareType } from 'lib-common/generated/api-types/serviceneed'
import { DailyServiceTimesType } from 'lib-common/generated/enums'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import { waitUntilEqual, waitUntilTrue } from '../../utils'
import {
  Checkbox,
  Combobox,
  DatePicker,
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

  readonly confirmButton = this.page.find(
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
      .find('[data-qa="person-guardians-collapsible"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async clickEdit() {
    await this.#editButton.click()
  }

  async deceasedIconIsShown() {
    await this.#deceased.waitUntilVisible()
  }

  async assertOphPersonOid(expected: string) {
    await this.#ophPersonOidInput.assertTextEquals(expected)
  }

  async setOphPersonOid(text: string) {
    await this.#ophPersonOidInput.click()
    await this.#ophPersonOidInput.type(text)
    await this.confirmButton.click()
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
  medication = this.find('[data-qa="medication"]')
  editBtn = this.find('[data-qa="edit-child-settings-button"]')
  medicationInput = new TextInput(this.find('[data-qa="medication-input"]'))
  confirmBtn = this.find('[data-qa="confirm-edited-child-button"]')

  readonly languageAtHome = this.page.findByDataQa('person-language-at-home')
  readonly languageAtHomeCombobox = new Combobox(
    this.page.findByDataQa('input-language-at-home')
  )

  readonly languageAtHomeDetails = this.page.findByDataQa(
    'person-language-at-home-details'
  )
  readonly languageAtHomeDetailsInput = new TextInput(
    this.page.findByDataQa('input-language-at-home-details')
  )
}

class DailyServiceTimeSectionBaseForm extends Section {
  async checkType(type: DailyServiceTimesType) {
    await this.findByDataQa(`radio-${type.toLowerCase()}`).click()
  }

  async fillRegularTimeRange(start: string, end: string) {
    await new TextInput(this.findByDataQa('regular-start')).fill(start)
    await new TextInput(this.findByDataQa('regular-end')).fill(end)
  }

  async fillIrregularTimeRange(day: string, start: string, end: string) {
    await new TextInput(this.findByDataQa(`${day}-start`)).fill(start)
    await new TextInput(this.findByDataQa(`${day}-end`)).fill(end)
  }
}

class DailyServiceTimeSectionCreationForm extends DailyServiceTimeSectionBaseForm {
  readonly validityPeriodStart = new DatePicker(
    this.findByDataQa('daily-service-times-validity-period-start')
  )

  async submit() {
    await this.findByDataQa('create-times-btn').click()
  }
}

class DailyServiceTimeSectionEditForm extends DailyServiceTimeSectionBaseForm {
  async submit() {
    await this.findByDataQa('modify-times-btn').click()
  }
}

export class DailyServiceTimeSection extends Section {
  readonly #createButton = this.findByDataQa('create-daily-service-times')

  private getNthRow(nth: number) {
    return this.findAllByDataQa('daily-service-times-row').nth(nth)
  }

  async create() {
    await this.#createButton.click()

    return new DailyServiceTimeSectionCreationForm(this.page, this.find('form'))
  }

  async assertTableRow(nth: number, title: string, status: string) {
    const row = this.findAllByDataQa('daily-service-times-row').nth(nth)

    await row
      .findByDataQa('daily-service-times-row-title')
      .assertTextEquals(title)
    await row
      .findByDataQa('status')
      .assertAttributeEquals('data-qa-status', status)
  }

  async toggleTableRowCollapsible(nth: number) {
    await this.getNthRow(nth)
      .findByDataQa('daily-service-times-row-opener')
      .click()
  }

  async assertTableRowCollapsible(nth: number, text: string) {
    const collapsible = this.find(
      `:nth-match([data-qa="daily-service-times-row"], ${
        nth + 1
      }) + [data-qa="daily-service-times-row-collapsible"]`
    )

    await collapsible.assertTextEquals(text)
  }

  async editTableRow(nth: number) {
    await this.getNthRow(nth)
      .findByDataQa('daily-service-times-row-edit')
      .click()

    const editor = this.find(
      `:nth-match([data-qa="daily-service-times-row"], ${
        nth + 1
      }) + [data-qa="daily-service-times-row-editor"]`
    )

    return new DailyServiceTimeSectionEditForm(this.page, editor)
  }

  async deleteTableRow(nth: number) {
    await this.getNthRow(nth)
      .findByDataQa('daily-service-times-row-delete')
      .click()

    await this.page.findByDataQa('modal-okBtn').click()
  }

  async assertTableRowCount(count: number) {
    await this.findAllByDataQa('daily-service-times-row').assertCount(count)
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
    return this.#startDate.text
  }

  get document(): Promise<string> {
    return this.#document.text
  }

  get description(): Promise<string> {
    return this.#description.text
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

  #backupCares = this.find('[data-qa="backup-cares"]')

  #error = this.findByDataQa('form-error')

  async #fillBackupCareFields(
    daycareName: string | undefined,
    startDate: LocalDate,
    endDate: LocalDate
  ) {
    if (daycareName !== undefined) {
      await this.#backupCareSelectUnit.fillAndSelectFirst(daycareName)
    }
    await this.#startDate.fill(startDate)
    await this.#endDate.fill(endDate)
  }

  async createBackupCare(
    daycareName: string,
    startDate: LocalDate,
    endDate: LocalDate
  ) {
    await this.fillNewBackupCareFields(daycareName, startDate, endDate)
    await this.find('[data-qa="submit-backup-care-form"]').click()
    await this.#backupCares.waitUntilVisible()
  }

  async fillNewBackupCareFields(
    daycareName: string,
    startDate: LocalDate,
    endDate: LocalDate
  ) {
    await this.#createBackupCareButton.click()
    await this.#fillBackupCareFields(daycareName, startDate, endDate)
  }

  async fillExistingBackupCareRow(
    rowIndex: number,
    startDate: LocalDate,
    endDate: LocalDate
  ) {
    await this.#backupCares
      .findAllByDataQa('backup-care-row')
      .nth(rowIndex)
      .findByDataQa('btn-edit-backup-care')
      .click()
    await this.#fillBackupCareFields(undefined, startDate, endDate)
  }

  async assertError(expectedError: string) {
    await this.#error.assertTextEquals(expectedError)
  }

  async getBackupCares(): Promise<Array<{ unit: string; period: string }>> {
    await this.#backupCares.waitUntilVisible()
    return this.evaluate((el) =>
      Array.from(el.querySelectorAll('[data-qa="backup-care-row"]')).map(
        (row) => ({
          unit: row.querySelector('[data-qa="unit"]')?.textContent ?? '',
          period: row.querySelector('[data-qa="period"]')?.textContent ?? ''
        })
      )
    )
  }

  async deleteBackupCare(index: number) {
    await this.#backupCareRow(index)
      .find('[data-qa="btn-remove-backup-care"]')
      .click()

    const modal = this.#backupCares.find('[data-qa="modal"]')
    await modal.waitUntilVisible()
    await this.#backupCares.find('[data-qa="modal-okBtn"]').click()
    await modal.waitUntilHidden()
  }

  #backupCareRow(index: number) {
    return this.#backupCares.findAllByDataQa('backup-care-row').nth(index)
  }
}

interface FamilyContactDetails {
  email: string
  phone: string
  backupPhone: string
}

export class FamilyContactsSection extends Section {
  #row(name: string) {
    return this.find(`[data-qa="table-backup-pickup-row-${name}"]`)
  }

  #familyContactRow(id: string) {
    return this.find(`[data-qa="table-family-contact-row-${id}"]`)
  }

  #createBtn = this.find('[data-qa="create-backup-pickup-btn"]')
  #nameInput = new TextInput(this.find('[data-qa="backup-pickup-name-input"]'))
  #phoneInput = new TextInput(
    this.find('[data-qa="backup-pickup-phone-input"]')
  )
  #modalOk = this.find('[data-qa="modal-okBtn"]')

  async modifyFamilyContactDetails(id: string, data: FamilyContactDetails) {
    const row = this.#familyContactRow(id)
    await row.findByDataQa('family-contact-edit').click()

    await new TextInput(row.findByDataQa('family-contact-email-input')).fill(
      data.email
    )
    await new TextInput(row.findByDataQa('family-contact-phone-input')).fill(
      data.phone
    )
    await new TextInput(
      row.findByDataQa('family-contact-backup-phone-input')
    ).fill(data.backupPhone)

    await row.findByDataQa('family-contact-save').click()
  }

  async assertFamilyContactDetails(id: string, data: FamilyContactDetails) {
    const row = this.#familyContactRow(id)
    await row.waitUntilVisible()

    if (data.email) {
      await row
        .findByDataQa('family-contact-email')
        .assertTextEquals(data.email)
    } else {
      await row.findByDataQa('family-contact-email').waitUntilHidden()
    }
    if (data.phone) {
      await row
        .findByDataQa('family-contact-phone')
        .assertTextEquals(data.phone)
    } else {
      await row.findByDataQa('family-contact-phone').waitUntilHidden()
    }
    if (data.backupPhone) {
      await row
        .findByDataQa('family-contact-backup-phone')
        .assertTextEquals(`${data.backupPhone} (Varanro)`)
    } else {
      await row.findByDataQa('family-contact-backup-phone').waitUntilHidden()
    }
  }

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
  private guardianRow = (id: UUID) =>
    this.findByDataQa(`table-guardian-row-${id}`)

  async assertGuardianExists(id: UUID) {
    await this.guardianRow(id).waitUntilVisible()
  }

  async assertFosterParentExists(
    parentId: string,
    start: LocalDate,
    end: LocalDate | null
  ) {
    const row = this.findByDataQa(`foster-parent-row-${parentId}`)
    await row.findByDataQa('start').assertTextEquals(start.format())
    await row.findByDataQa('end').assertTextEquals(end?.format() ?? '')
  }

  async removeGuardianEvakaRights(id: UUID) {
    await this.guardianRow(id)
      .findByDataQa('edit-guardian-evaka-rights')
      .click()

    const modal = new Modal(this.page.findByDataQa('evaka-rights-modal'))
    await modal.submit()
    await modal.findByDataQa('confirmation').click()
    await modal.findByDataQa('denied').click()
    await modal.submit()
  }

  async restoreGuardianEvakaRights(id: UUID) {
    await this.guardianRow(id)
      .findByDataQa('edit-guardian-evaka-rights')
      .click()

    const modal = new Modal(this.page.findByDataQa('evaka-rights-modal'))
    await modal.submit()
    await modal.findByDataQa('denied').click()
    await modal.findByDataQa('confirmation').click()
    await modal.submit()
  }

  async waitUntilNotLoading() {
    await waitUntilEqual(
      () =>
        this.findByDataQa('table-of-guardians').getAttribute('data-loading'),
      'false'
    )
  }

  async assertGuardianStatusAllowed(id: UUID) {
    await this.waitUntilNotLoading()
    await this.guardianRow(id)
      .findByDataQa('evaka-rights-status')
      .assertTextEquals('Sallittu')
  }

  async assertGuardianStatusDenied(id: UUID) {
    await this.waitUntilNotLoading()
    await this.guardianRow(id)
      .findByDataQa('evaka-rights-status')
      .assertTextEquals('Kielletty')
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

  #nthServiceNeedEditButton = (index: number) =>
    this.#serviceNeedRow(index).findByDataQa('service-need-edit')

  #serviceNeedShiftCareCheckBox = new Checkbox(
    this.findByDataQa('shift-care-toggle')
  )

  #shiftTypes = ['NONE', 'INTERMITTENT', 'FULL']
  #serviceNeedShiftCareRadios = this.#shiftTypes.map(
    (type) => new Radio(this.findByDataQa(`shift-care-type-radio-${type}`))
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

  async addMissingServiceNeed(
    placementId: string,
    optionName: string,
    shiftCare: ShiftCareType = 'NONE',
    intermittentShiftCare = false
  ) {
    await this.openPlacement(placementId)
    await this.#addMissingServiceNeedButton.click()
    await this.#serviceNeedOptionSelect.selectOption({ label: optionName })

    if (intermittentShiftCare) {
      await this.#serviceNeedShiftCareRadios[
        this.#shiftTypes.indexOf(shiftCare)
      ].check()
    } else {
      if (shiftCare === 'FULL') {
        await this.#serviceNeedShiftCareCheckBox.check()
      }
    }
    await this.#serviceNeedSaveButton.click()
  }

  async assertNthServiceNeedName(index: number, optionName: string) {
    await this.#serviceNeedRowOptionName(index).assertTextEquals(optionName)
  }

  async assertNthServiceNeedShiftCare(
    index: number,
    shiftCareType: ShiftCareType
  ) {
    await this.#serviceNeedRow(0)
      .findByDataQa(`shift-care-${shiftCareType}`)
      .waitUntilVisible()
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

  async setShiftCareTypeOfNthServiceNeed(
    index: number,
    shiftCareType: ShiftCareType
  ) {
    await this.#nthServiceNeedEditButton(index).click()
    await this.#serviceNeedShiftCareRadios[
      this.#shiftTypes.indexOf(shiftCareType)
    ].check()
    await this.#serviceNeedSaveButton.click()
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
    await this.#assistanceNeedMultiplier.nth(nth).assertTextEquals(expected)
  }

  async assertAssistanceNeedCount(expectedCount: number) {
    await waitUntilEqual(() => this.#assistanceNeedRow.count(), expectedCount)
  }

  async waitUntilAssistanceNeedDecisionsLoaded() {
    await this.page
      .findByDataQa('table-of-assistance-need-decisions')
      .waitUntilVisible()
  }

  async assistanceNeedDecisions(nth: number) {
    const row = this.page
      .findByDataQa('table-of-assistance-need-decisions')
      .findAllByDataQa('table-assistance-need-decision-row')
      .nth(nth)

    return {
      date: await row.findByDataQa('assistance-need-decision-date').text,
      unitName: await row.findByDataQa('assistance-need-decision-unit-name')
        .text,
      sentDate: await row.findByDataQa('assistance-need-decision-sent-date')
        .text,
      decisionMadeDate: await row.findByDataQa(
        'assistance-need-decision-made-date'
      ).text,
      status: await row
        .findByDataQa('decision-status')
        .getAttribute('data-qa-status'),
      actionCount: await row
        .findByDataQa('assistance-need-decision-actions')
        .findAll('button')
        .count()
    }
  }

  readonly assistanceNeedVoucherCoefficientCount = () =>
    this.page
      .findByDataQa('table-of-assistance-need-voucher-coefficients')
      .findAllByDataQa('table-assistance-need-voucher-coefficient')
      .count()

  async assistanceNeedVoucherCoefficients(nth: number) {
    const row = this.page
      .findByDataQa('table-of-assistance-need-voucher-coefficients')
      .findAllByDataQa('table-assistance-need-voucher-coefficient')
      .nth(nth)

    return {
      coefficient: await row.findByDataQa(
        'assistance-need-voucher-coefficient-coefficient'
      ).text,
      validityPeriod: await row.findByDataQa(
        'assistance-need-voucher-coefficient-validity-period'
      ).text,
      status: await row
        .findByDataQa('assistance-need-voucher-coefficient-status')
        .getAttribute('data-qa-status'),
      actionCount: await row
        .findByDataQa('assistance-need-voucher-coefficient-actions')
        .findAll('button')
        .count()
    }
  }

  assistanceNeedVoucherCoefficientActions(nth: number) {
    const row = this.page
      .findByDataQa('table-of-assistance-need-voucher-coefficients')
      .findAllByDataQa('table-assistance-need-voucher-coefficient')
      .nth(nth)

    return {
      editBtn: row.findByDataQa('assistance-need-voucher-coefficient-edit-btn'),
      deleteBtn: row.findByDataQa(
        'assistance-need-voucher-coefficient-delete-btn'
      )
    }
  }

  assistanceNeedVoucherCoefficientForm(container: Element) {
    const validityPeriod = container.findByDataQa(
      'input-assistance-need-voucher-coefficient-validity-period'
    )

    return {
      coefficientInput: new TextInput(
        container.findByDataQa('input-assistance-need-voucher-coefficient')
      ),
      validityPeriod: {
        startInput: new TextInput(validityPeriod.findByDataQa('start-date')),
        endInput: new TextInput(validityPeriod.findByDataQa('end-date'))
      },
      saveBtn: container.findByDataQa(
        'assistance-need-voucher-coefficient-save'
      )
    }
  }

  readonly createAssistanceNeedVoucherCoefficientBtn = this.page.findByDataQa(
    'assistance-need-voucher-coefficient-create-btn'
  )
  readonly createAssistanceNeedVoucherCoefficientForm = this.page.findByDataQa(
    'create-new-assistance-need-voucher-coefficient'
  )
  readonly editAssistanceNeedVoucherCoefficientForm = this.page.findByDataQa(
    'table-assistance-need-voucher-coefficient-editor'
  )

  readonly modalOkBtn = this.page.findByDataQa('modal-okBtn')

  readonly createPedagogicalAssessmentButton = this.page.findByDataQa(
    'create-PEDAGOGICAL_ASSESSMENT'
  )
  readonly createPedagogicalReportButton = this.page.findByDataQa(
    'create-PEDAGOGICAL_REPORT'
  )

  readonly childDocumentsCount = () =>
    this.page
      .findByDataQa('table-of-child-documents')
      .findAllByDataQa('child-document-row')
      .count()

  childDocuments(nth: number) {
    const row = this.page
      .findByDataQa('table-of-child-documents')
      .findAllByDataQa('child-document-row')
      .nth(nth)

    return {
      openLink: row.findByDataQa('open-document'),
      status: row.findByDataQa('document-status')
    }
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

export class ConsentsSection extends Section {
  readonly evakaProfilePicYes = new Radio(
    this.page.findByDataQa('consent-profilepic-yes')
  )
  readonly evakaProfilePicNo = new Radio(
    this.page.findByDataQa('consent-profilepic-no')
  )
  readonly evakaProfilePicModifiedBy = this.page.findByDataQa(
    'consent-profilepic-modified-by'
  )
  readonly evakaProfilePicClear = this.page.findByDataQa(
    'consent-profilepic-clear'
  )

  #confirmButton = this.page.findByDataQa('consent-confirm')

  async save() {
    await this.#confirmButton.click()
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
    selector: '[data-qa="family-contacts-collapsible"][data-isloading="false"]',
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
  },
  consents: {
    selector: '[data-qa="consents-collapsible"]',
    section: ConsentsSection
  }
}

type Collapsibles = typeof collapsibles
type Collapsible = keyof Collapsibles
type SectionFor<C extends Collapsible> = InstanceType<
  Collapsibles[C]['section']
>
