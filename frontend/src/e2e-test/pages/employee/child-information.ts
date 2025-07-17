// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type FiniteDateRange from 'lib-common/finite-date-range'
import type { DailyServiceTimesType } from 'lib-common/generated/api-types/dailyservicetimes'
import type { ShiftCareType } from 'lib-common/generated/api-types/serviceneed'
import { shiftCareType } from 'lib-common/generated/api-types/serviceneed'
import type HelsinkiDateTime from 'lib-common/helsinki-date-time'
import type LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import config from '../../config'
import { waitUntilEqual, waitUntilTrue } from '../../utils'
import type { Page } from '../../utils/page'
import {
  Checkbox,
  Combobox,
  DatePicker,
  DateRangePicker,
  Element,
  FileUpload,
  Modal,
  Radio,
  Select,
  TextInput
} from '../../utils/page'

import CreateApplicationModal from './applications/create-application-modal'
import { ChildDocumentPage } from './documents/child-document'
import { IncomeSection } from './guardian-information'

export default class ChildInformationPage {
  #deceased: Element
  #ophPersonOidInput: TextInput
  #editButton: Element
  confirmButton: Element

  constructor(private readonly page: Page) {
    this.#deceased = page.findByDataQa('deceased-label')
    this.#ophPersonOidInput = new TextInput(
      page.findByDataQa('person-oph-person-oid')
    )
    this.#editButton = page.findByDataQa('edit-person-settings-button')
    this.confirmButton = page.findByDataQa('confirm-edited-person-button')
  }

  async navigateToChild(id: UUID) {
    await this.page.goto(config.employeeUrl + '/child-information/' + id)
  }

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="person-details-section"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async assertName(lastName: string, firstName: string) {
    await this.page.findByDataQa('person-last-name').assertTextEquals(lastName)
    await this.page
      .findByDataQa('person-first-names')
      .assertTextEquals(firstName)
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
      this.page.findByDataQa('additional-information-section')
    )
  }
}

class Section extends Element {
  constructor(
    protected page: Page,
    root: Element
  ) {
    super(root)
  }
}

export class AdditionalInformationSection extends Section {
  languageAtHome: Element
  languageAtHomeCombobox: Combobox
  languageAtHomeDetails: Element
  languageAtHomeDetailsInput: TextInput
  specialDietCombobox: Combobox
  participatesInBreakfastCheckbox: Checkbox
  nekkuDietSelect: Combobox

  constructor(page: Page, root: Element) {
    super(page, root)
    this.languageAtHome = page.findByDataQa('person-language-at-home')
    this.languageAtHomeCombobox = new Combobox(
      page.findByDataQa('input-language-at-home')
    )
    this.languageAtHomeDetails = page.findByDataQa(
      'person-language-at-home-details'
    )
    this.languageAtHomeDetailsInput = new TextInput(
      page.findByDataQa('input-language-at-home-details')
    )
    this.specialDietCombobox = new Combobox(page.findByDataQa('diet-input'))
    this.participatesInBreakfastCheckbox = new Checkbox(
      page.findByDataQa('participates-in-breakfast-checkbox')
    )
    this.nekkuDietSelect = new Combobox(page.findByDataQa('nekku-diet-input'))
  }

  medication = this.find('[data-qa="medication"]')
  editBtn = this.find('[data-qa="edit-child-settings-button"]')
  medicationInput = new TextInput(this.find('[data-qa="medication-input"]'))
  confirmBtn = this.find('[data-qa="confirm-edited-child-button"]')
  participatesInBreakfast = this.find(
    '[data-qa="participates-in-breakfast-display"]'
  )
  nekkuDiet = this.find('[data-qa="nekku-diet-display"]')

  readonly specialDiet = this.page.findByDataQa('diet-value-display')

  getNekkuSpecialDietEditor() {
    return new NekkuSpecialDietEditor(
      this.findByDataQa('nekku-special-diet-editor')
    )
  }
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

export class AbsenceApplicationsSection extends Section {
  async assertIncompleted(expected: string[]) {
    const table = this.page.findByDataQa('absence-applications-incompleted')
    const rows = table.findAllByDataQa('absence-applications-incompleted-row')
    await rows.assertTextsEqual(expected)
  }

  async assertCompleted(expected: string[]) {
    const table = this.page.findByDataQa('absence-applications-completed')
    const rows = table.findAllByDataQa('absence-applications-completed-row')
    await rows.assertTextsEqual(expected)
  }

  async openRejectModal(index: number) {
    const table = this.page.findByDataQa('absence-applications-incompleted')
    const rows = table.findAllByDataQa('absence-applications-incompleted-row')
    await rows.nth(index).findByDataQa('reject-absence-application').click()
    return new RejectAbsenceApplicationModal(
      this.page.findByDataQa('reject-absence-application-modal')
    )
  }

  async accept(index: number) {
    const table = this.page.findByDataQa('absence-applications-incompleted')
    const rows = table.findAllByDataQa('absence-applications-incompleted-row')
    await rows.nth(index).findByDataQa('accept-absence-application').click()
  }
}

class RejectAbsenceApplicationModal extends Modal {
  readonly reason: TextInput

  constructor(self: Element) {
    super(self)
    this.reason = new TextInput(self.findByDataQa('reason'))
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

export class PedagogicalDocumentsSection extends Section {
  readonly #startDate = this.find('[data-qa="pedagogical-document-start-date"]')

  readonly #document = this.find('[data-qa="pedagogical-document-document"]')

  readonly #descriptionInput = new TextInput(
    this.find('[data-qa="pedagogical-document-description"]')
  )

  readonly #description = this.find(
    '[data-qa="pedagogical-document-description"]'
  )

  readonly fileUpload = new FileUpload(
    this.findByDataQa('upload-pedagogical-document-attachment-new')
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
}

export class ChildDocumentsSection extends Section {
  createInternalDocumentButton: Element
  createDecisionDocumentButton: Element
  createExternalDocumentButton: Element
  createModalTemplateSelect: Select
  modalOk: Element

  constructor(page: Page, root: Element) {
    super(page, root)
    this.createInternalDocumentButton = page.findByDataQa(
      'create-internal-document'
    )
    this.createDecisionDocumentButton = page.findByDataQa(
      'create-decision-document'
    )
    this.createExternalDocumentButton = page.findByDataQa(
      'create-external-document'
    )
    this.createModalTemplateSelect = new Select(
      page.findByDataQa('template-select')
    )
    this.modalOk = page.findByDataQa('modal-okBtn')
  }

  async assertInternalChildDocuments(expectedRows: { id: UUID }[]) {
    const rows = this.page
      .findByDataQa('table-of-internal-child-documents')
      .findAllByDataQa('child-document-row')
    await rows.assertCount(expectedRows.length)
    await Promise.all(
      expectedRows.map(async (expected, index) => {
        const row = rows.nth(index)
        await row
          .findByDataQa(`child-document-${expected.id}`)
          .waitUntilVisible()
      })
    )
  }

  async openChildDocument(id: UUID) {
    await this.page
      .findByDataQa(`child-document-${id}`)
      .findByDataQa('open-document')
      .click()
    return new ChildDocumentPage(this.page)
  }

  readonly internalChildDocumentsCount = () =>
    this.page
      .findByDataQa('table-of-internal-child-documents')
      .findAllByDataQa('child-document-row')
      .count()

  internalChildDocuments(nth: number) {
    const row = this.page
      .findByDataQa('table-of-internal-child-documents')
      .findAllByDataQa('child-document-row')
      .nth(nth)

    return {
      openLink: row.findByDataQa('open-document'),
      status: row.findByDataQa('document-status'),
      published: row.findByDataQa('document-published-at')
    }
  }

  readonly externalChildDocumentsCount = () =>
    this.page
      .findByDataQa('table-of-external-child-documents')
      .findAllByDataQa('child-document-row')
      .count()

  externalChildDocuments(nth: number) {
    const row = this.page
      .findByDataQa('table-of-external-child-documents')
      .findAllByDataQa('child-document-row')
      .nth(nth)

    return {
      openLink: row.findByDataQa('open-document'),
      status: row.findByDataQa('document-status'),
      sent: row.findByDataQa('document-sent-at'),
      answered: row.findByDataQa('document-answered-at')
    }
  }

  readonly decisionChildDocumentsCount = () =>
    this.page
      .findByDataQa('table-of-decision-child-documents')
      .findAllByDataQa('child-document-row')
      .count()

  decisionChildDocuments(nth: number) {
    const row = this.page
      .findByDataQa('table-of-decision-child-documents')
      .findAllByDataQa('child-document-row')
      .nth(nth)

    return {
      openLink: row.findByDataQa('open-document'),
      status: row.findByDataQa('document-status'),
      validity: row.findByDataQa('decision-validity'),
      setDecisionValidity: async (start: LocalDate, end: LocalDate | null) => {
        await row.findByDataQa('edit-decision-validity').click()
        const picker = new DateRangePicker(
          this.findByDataQa('decision-validity-picker')
        )
        await picker.fill(start, end ?? '')
        await this.findByDataQa('modal-okBtn').click()
      }
    }
  }
}

export class BackupCaresSection extends Section {
  #createBackupCareButton = this.find('[data-qa="backup-care-create-btn"]')
  #backupCareSelectUnit = new Combobox(
    this.find('[data-qa="backup-care-select-unit"]')
  )

  #startDate = new DatePicker(this.findByDataQa('backup-care-start-date'))
  #endDate = new DatePicker(this.findByDataQa('backup-care-end-date'))

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

  async getBackupCares(): Promise<{ unit: string; period: string }[]> {
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
    await modal.findByDataQa('denied').click()
    await modal.findByDataQa('confirmation').click()
    await modal.submit()
  }

  async restoreGuardianEvakaRights(id: UUID) {
    await this.guardianRow(id)
      .findByDataQa('edit-guardian-evaka-rights')
      .click()

    const modal = new Modal(this.page.findByDataQa('evaka-rights-modal'))
    await modal.submit()
    await modal.findByDataQa('denied').click()
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
  #serviceNeedRowRange = (index: number) =>
    this.#serviceNeedRow(index).findByDataQa('service-need-range')
  #serviceNeedRowOptionName = (index: number) =>
    this.#serviceNeedRow(index).findByDataQa('service-need-name')
  #serviceNeedPartWeek = (index: number) =>
    this.#serviceNeedRow(index).findByDataQa('part-week')
  addMissingServiceNeedButton = this.find(
    '[data-qa="add-new-missing-service-need"]'
  )
  serviceNeedEndDate = new DatePicker(
    this.findByDataQa('service-need-range').findByDataQa('end-date')
  )
  serviceNeedOptionSelect = new Select(
    this.find('[data-qa="service-need-option-select"]')
  )
  serviceNeedPartWeekCheckbox = new Checkbox(
    this.findByDataQa('part-week-checkbox')
  )

  #nthServiceNeedEditButton = (index: number) =>
    this.#serviceNeedRow(index).findByDataQa('service-need-edit')

  #serviceNeedShiftCareCheckBox = new Checkbox(
    this.findByDataQa('shift-care-toggle')
  )

  #serviceNeedShiftCareRadios = shiftCareType.map(
    (type) => new Radio(this.findByDataQa(`shift-care-type-radio-${type}`))
  )

  serviceNeedSaveButton = this.find('[data-qa="service-need-save"]')
  #terminatedByGuardian = (placementId: string) =>
    this.#placementRow(placementId).find('[data-qa="placement-terminated"]')
  partiallyInvalidWarning = this.findByDataQa('partially-invalid-warning')

  async assertPlacementRows(
    rows: { unitName: string; period: string; status: string }[]
  ) {
    const placements = this.findAllByDataQa('placement-row')
    await placements.assertCount(rows.length)
    await Promise.all(
      rows.map(async (row, index) => {
        const placement = placements.nth(index)
        await placement
          .findByDataQa('toolbar-accordion-title')
          .assertTextEquals(row.unitName)
        await placement
          .findByDataQa('toolbar-accordion-subtitle')
          .assertTextEquals(row.period)
        await placement
          .findByDataQa('placement-toolbar')
          .assertTextEquals(row.status)
      })
    )
  }

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
    intermittentShiftCare = false,
    partWeek: boolean | null = null
  ) {
    await this.openPlacement(placementId)
    await this.addMissingServiceNeedButton.click()
    await this.serviceNeedOptionSelect.selectOption({ label: optionName })

    if (intermittentShiftCare) {
      const indexOfType = shiftCareType.indexOf(shiftCare)
      await this.#serviceNeedShiftCareRadios[indexOfType].check()
    } else {
      if (shiftCare === 'FULL') {
        await this.#serviceNeedShiftCareCheckBox.check()
      }
    }

    if (partWeek === true) {
      await this.serviceNeedPartWeekCheckbox.check()
    } else if (partWeek === false) {
      await this.serviceNeedPartWeekCheckbox.uncheck()
    } else {
      await this.serviceNeedPartWeekCheckbox.waitUntilHidden()
    }

    await this.serviceNeedSaveButton.click()
  }

  async assertNthServiceNeedRange(index: number, range: FiniteDateRange) {
    await this.#serviceNeedRowRange(index).assertTextEquals(range.format())
  }

  async assertNthServiceNeedName(index: number, optionName: string) {
    await this.#serviceNeedRowOptionName(index).assertTextEquals(optionName)
  }

  async assertNthServiceNeedPartWeek(index: number, partWeek: boolean) {
    await this.#serviceNeedPartWeek(index).assertTextEquals(
      partWeek ? 'Kyllä' : 'Ei'
    )
  }

  async assertNthServiceNeedShiftCare(
    index: number,
    shiftCareType: ShiftCareType
  ) {
    await this.#serviceNeedRow(index)
      .findByDataQa(`shift-care-${shiftCareType}`)
      .waitUntilVisible()
  }

  async assertServiceNeedOptions(placementId: string, optionIds: string[]) {
    await this.openPlacement(placementId)
    await this.addMissingServiceNeedButton.click()
    await waitUntilTrue(async () => {
      const selectableOptions = await this.serviceNeedOptionSelect
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
    endDate,
    placeGuarantee = false
  }: {
    unitName: string
    startDate: string
    endDate: string
    placeGuarantee?: boolean
  }) {
    await this.findByDataQa('create-new-placement-button').click()

    const modal = new Modal(this.page.findByDataQa('modal'))
    const unitSelect = new Combobox(modal.findByDataQa('unit-select'))
    await unitSelect.fillAndSelectFirst(unitName)

    const start = new DatePicker(
      modal.findByDataQa('create-placement-start-date')
    )
    await start.fill(startDate)

    const end = new DatePicker(modal.findByDataQa('create-placement-end-date'))
    await end.fill(endDate)

    if (placeGuarantee) {
      await new Checkbox(
        modal.findByDataQa('create-placement-place-guarantee')
      ).check()
    }

    await modal.submit()
  }

  async setShiftCareTypeOfNthServiceNeed(
    index: number,
    shiftCare: ShiftCareType
  ) {
    await this.#nthServiceNeedEditButton(index).click()
    const indexOfType = shiftCareType.indexOf(shiftCare)
    await this.#serviceNeedShiftCareRadios[indexOfType].check()
    await this.serviceNeedSaveButton.click()
  }
}

export class ServiceApplicationsSection extends Section {
  undecidedApplication: Element

  constructor(page: Page, root: Element) {
    super(page, root)
    this.undecidedApplication = page.findByDataQa('undecided-application')
  }

  async assertUndecidedApplication(
    startDate: string,
    serviceNeed: string,
    additionalInfo: string
  ) {
    await this.undecidedApplication
      .findByDataQa('start-date')
      .assertTextEquals(startDate)
    await this.undecidedApplication
      .findByDataQa('service-need')
      .assertTextEquals(serviceNeed)
    await this.undecidedApplication
      .findByDataQa('additional-info')
      .assertTextEquals(additionalInfo)
  }

  async acceptApplication() {
    await this.undecidedApplication
      .findByDataQa('accept-application-button')
      .click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async rejectApplication(reason: string) {
    await this.undecidedApplication
      .findByDataQa('reject-application-button')
      .click()
    await new TextInput(this.page.findByDataQa('reject-reason')).fill(reason)
    await this.page.findByDataQa('modal-okBtn').click()
  }

  decidedApplication = (n: number) =>
    this.page.findAllByDataQa('decided-application-row').nth(n)

  async assertDecidedApplication(
    n: number,
    startDate: string,
    serviceNeed: string,
    status: string,
    decidedAt: HelsinkiDateTime
  ) {
    await this.decidedApplication(n)
      .findByDataQa('start-date')
      .assertTextEquals(startDate)
    await this.decidedApplication(n)
      .findByDataQa('service-need')
      .assertTextEquals(serviceNeed)
    await this.decidedApplication(n)
      .findByDataQa('decision-status')
      .assertTextEquals(`${status}, ${decidedAt.toLocalDate().format()}`)
  }
}

export class AssistanceSection extends Section {
  createAssistanceNeedVoucherCoefficientBtn: Element
  createAssistanceNeedVoucherCoefficientForm: Element
  editAssistanceNeedVoucherCoefficientForm: Element

  constructor(page: Page, root: Element) {
    super(page, root)
    this.createAssistanceNeedVoucherCoefficientBtn = page.findByDataQa(
      'assistance-need-voucher-coefficient-create-btn'
    )
    this.createAssistanceNeedVoucherCoefficientForm = page.findByDataQa(
      'create-new-assistance-need-voucher-coefficient'
    )
    this.editAssistanceNeedVoucherCoefficientForm = page.findByDataQa(
      'table-assistance-need-voucher-coefficient-editor'
    )
  }

  createAssistanceFactorButton = this.findByDataQa(
    'assistance-factor-create-btn'
  )
  createDaycareAssistanceButton = this.findByDataQa(
    'daycare-assistance-create-btn'
  )
  createPreschoolAssistanceButton = this.findByDataQa(
    'preschool-assistance-create-btn'
  )
  createOtherAssistanceMeasureButton = this.findByDataQa(
    'other-assistance-measure-create-btn'
  )
  assistanceFactorForm = new AssistanceFactorForm(
    this.findByDataQa('assistance-factor-form')
  )
  daycareAssistanceForm = new DaycareAssistanceForm(
    this.findByDataQa('daycare-assistance-form')
  )
  preschoolAssistanceForm = new PreschoolAssistanceForm(
    this.findByDataQa('preschool-assistance-form')
  )
  otherAssistanceMeasureForm = new OtherAssistanceMeasureForm(
    this.findByDataQa('other-assistance-measure-form')
  )
  #assistanceFactorRows = this.findAllByDataQa('assistance-factor-row')
  #daycareAssistanceRows = this.findAllByDataQa('daycare-assistance-row')
  #preschoolAssistanceRows = this.findAllByDataQa('preschool-assistance-row')
  #otherAssistanceMeasureRows = this.findAllByDataQa(
    'other-assistance-measure-row'
  )

  assistanceFactorRow(nth: number): AssistanceFactorRow {
    return new AssistanceFactorRow(this.#assistanceFactorRows.nth(nth))
  }

  async assertAssistanceFactorCount(count: number) {
    await this.#assistanceFactorRows.assertCount(count)
  }

  daycareAssistanceRow(nth: number): DaycareAssistanceRow {
    return new DaycareAssistanceRow(this.#daycareAssistanceRows.nth(nth))
  }

  async assertDaycareAssistanceCount(count: number) {
    await this.#daycareAssistanceRows.assertCount(count)
  }

  preschoolAssistanceRow(nth: number): PreschoolAssistanceRow {
    return new PreschoolAssistanceRow(this.#preschoolAssistanceRows.nth(nth))
  }

  async assertPreschoolAssistanceCount(count: number) {
    await this.#preschoolAssistanceRows.assertCount(count)
  }

  otherAssistanceMeasureRow(nth: number): OtherAssistanceMeasureRow {
    return new OtherAssistanceMeasureRow(
      this.#otherAssistanceMeasureRows.nth(nth)
    )
  }

  async assertOtherAssistanceMeasureCount(count: number) {
    await this.#otherAssistanceMeasureRows.assertCount(count)
  }

  async waitUntilAssistanceNeedDecisionsLoaded() {
    await this.page
      .findByDataQa('table-of-assistance-need-decisions')
      .waitUntilVisible()
  }

  async assertAssistanceNeedDecisionCount(count: number) {
    const rows = this.page.findAllByDataQa('table-assistance-need-decision-row')
    await rows.assertCount(count)
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

  async assistanceNeedVoucherCoefficientModified(nth: number) {
    const row = this.page
      .findByDataQa('table-of-assistance-need-voucher-coefficients')
      .findAllByDataQa('table-assistance-need-voucher-coefficient')
      .nth(nth)

    return await row.findByDataQa('assistance-need-voucher-coefficient-status')
      .text
  }

  readonly modalOkBtn = this.page.findByDataQa('modal-okBtn')
}

abstract class InlineAssistanceForm extends Element {
  validDuring = this.findByDataQa('valid-during')
  startDate = new DatePicker(this.validDuring.findByDataQa('start-date'))
  endDate = new DatePicker(this.validDuring.findByDataQa('end-date'))
  save = this.findByDataQa('save')
  cancel = this.findByDataQa('cancel')

  async fillValidDuring(dateRange: FiniteDateRange) {
    await this.startDate.fill(dateRange.start.format())
    await this.endDate.fill(dateRange.end.format())
  }
}

abstract class InlineAssistanceRow extends Element {
  validDuring = this.findByDataQa('valid-during')
  edit = this.findByDataQa('edit')
  #deleteButton = this.findByDataQa('delete')

  async delete() {
    await this.#deleteButton.click()
    const modal = new Modal(this.locator)
    await modal.submit()
  }
}

class AssistanceFactorForm extends InlineAssistanceForm {
  capacityFactor = new TextInput(this.findByDataQa('capacity-factor'))
}

class AssistanceFactorRow extends InlineAssistanceRow {
  capacityFactor = this.findByDataQa('capacity-factor')
}

class DaycareAssistanceForm extends InlineAssistanceForm {
  level = new Select(this.findByDataQa('level'))
}

class DaycareAssistanceRow extends InlineAssistanceRow {
  level = this.findByDataQa('level')
}

class PreschoolAssistanceForm extends InlineAssistanceForm {
  level = new Select(this.findByDataQa('level'))
}

class PreschoolAssistanceRow extends InlineAssistanceRow {
  level = this.findByDataQa('level')
}

class OtherAssistanceMeasureForm extends InlineAssistanceForm {
  type = new Select(this.findByDataQa('type'))
}

class OtherAssistanceMeasureRow extends InlineAssistanceRow {
  type = this.findByDataQa('type')
}

class FeeAlterationEditorPage {
  dateRangePicker: DateRangePicker
  alterationValueInput: TextInput
  fileUpload: FileUpload
  saveButton: Element

  constructor(readonly page: Page) {
    this.dateRangePicker = new DateRangePicker(
      page.findByDataQa('fee-alteration-date-range-input')
    )
    this.alterationValueInput = new TextInput(
      page.findByDataQa('fee-alteration-amount-input')
    )
    this.fileUpload = new FileUpload(
      page.findByDataQa('fee-alteration-attachment-upload')
    )
    this.saveButton = page.findByDataQa('fee-alteration-editor-save-button')
  }

  async waitUntilReady() {
    await this.dateRangePicker.start.waitUntilVisible()
  }
}

export class FeeAlterationsSection extends Section {
  #createFeeAlterationButton = this.findByDataQa('create-fee-alteration-button')

  async openNewFeeAlterationEditorPage(): Promise<FeeAlterationEditorPage> {
    await this.#createFeeAlterationButton.click()
    const editorPage = new FeeAlterationEditorPage(this.page)
    await editorPage.waitUntilReady()
    return editorPage
  }

  async assertAlterationDateRange(expected: string, nth = 0) {
    const feeAlterationDates = this.findAllByDataQa('fee-alteration-dates')
    await feeAlterationDates.nth(nth).assertTextEquals(expected)
  }

  async assertAlterationAmount(expected: string, nth = 0) {
    const feeAlterationAmounts = this.findAllByDataQa('fee-alteration-amount')
    await feeAlterationAmounts.nth(nth).assertTextEquals(expected)
  }

  async assertAttachmentExists(name: string) {
    await waitUntilTrue(async () =>
      (await this.page.findAllByDataQa('attachment').allTexts()).includes(name)
    )
  }
}

export class NekkuSpecialDietEditor extends Element {
  getCheckBox(dietId: string, fieldId: string, optionKey: string) {
    return new Checkbox(
      this.findByDataQa(`${dietId}-${fieldId}-${optionKey}-checkbox`)
    )
  }

  getCheckBoxValue(dietId: string, fieldId: string) {
    return this.findByDataQa(`${dietId}-${fieldId}-value`)
  }

  getTextField(dietId: string, fieldId: string) {
    return new TextInput(this.findByDataQa(`${dietId}-${fieldId}-textarea`))
  }

  getTextValue(dietId: string, fieldId: string) {
    return this.findByDataQa(`${dietId}-${fieldId}-value`)
  }
}

class ApplicationsSection extends Section {
  #createApplication = this.find('[data-qa="button-create-application"]')

  async openCreateApplicationModal() {
    await this.#createApplication.click()
    return new CreateApplicationModal(
      this.page,
      this.page.findByDataQa('modal')
    )
  }
}

const collapsibles = {
  absenceApplications: {
    selector: '[data-qa="absence-applications-collapsible"]',
    section: AbsenceApplicationsSection
  },
  dailyServiceTimes: {
    selector: '[data-qa="child-daily-service-times-collapsible"]',
    section: DailyServiceTimeSection
  },
  pedagogicalDocuments: {
    selector: '[data-qa="pedagogical-documents-collapsible"]',
    section: PedagogicalDocumentsSection
  },
  childDocuments: {
    selector: '[data-qa="child-documents-collapsible"]',
    section: ChildDocumentsSection
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
  serviceApplications: {
    selector: '[data-qa="service-applications-collapsible"]',
    section: ServiceApplicationsSection
  },
  assistance: {
    selector: '[data-qa="assistance-collapsible"]',
    section: AssistanceSection
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
