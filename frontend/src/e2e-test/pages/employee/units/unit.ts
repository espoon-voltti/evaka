// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  ApplicationType,
  TransferApplicationUnitSummary
} from 'lib-common/generated/api-types/application'
import type { CareType } from 'lib-common/generated/api-types/daycare'
import type LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import config from '../../../config'
import { postPairingChallenge } from '../../../generated/api-clients'
import { waitUntilEqual, waitUntilTrue } from '../../../utils'
import type { Page } from '../../../utils/page'
import {
  Checkbox,
  Combobox,
  DatePicker,
  Element,
  Modal,
  TextInput,
  TreeDropdown
} from '../../../utils/page'
import ChildInformationPage from '../child-information'

import { UnitCalendarPageBase } from './unit-calendar-page-base'
import { DiscussionSurveyListPage } from './unit-discussion-survey-page'
import { UnitGroupsPage } from './unit-groups-page'
import { UnitWeekCalendarPage } from './unit-week-calendar-page'

type UnitProviderType =
  | 'MUNICIPAL'
  | 'PURCHASED'
  | 'PRIVATE'
  | 'MUNICIPAL_SCHOOL'
  | 'PRIVATE_SERVICE_VOUCHER'
  | 'EXTERNAL_PURCHASED'

export type MealTimes = {
  mealtimeEveningSnack?: { start: string; end: string }
  mealtimeBreakfast?: { start: string; end: string }
  mealtimeSnack?: { start: string; end: string }
  mealtimeLunch?: { start: string; end: string }
  mealtimeSupper?: { start: string; end: string }
}

export class UnitPage {
  #unitInfoTab: Element
  #groupsTab: Element
  #calendarTab: Element
  #applicationProcessTab: Element
  serviceWorkerNote: {
    addButton: Element
    editButton: Element
    saveButton: Element
    removeButton: Element
    content: Element
    input: TextInput
  }

  constructor(private readonly page: Page) {
    this.#unitInfoTab = page.findByDataQa('unit-info-tab')
    this.#groupsTab = page.findByDataQa('groups-tab')
    this.#calendarTab = page.findByDataQa('calendar-tab')
    this.#applicationProcessTab = page.findByDataQa('application-process-tab')
    this.serviceWorkerNote = {
      addButton: page.findByDataQa('note-add-btn'),
      editButton: page.findByDataQa('note-edit-btn'),
      saveButton: page.findByDataQa('note-save-btn'),
      removeButton: page.findByDataQa('note-remove-btn'),
      content: page.findByDataQa('service-worker-note'),
      input: new TextInput(page.findByDataQa('note-input'))
    }
  }

  static async openUnit(page: Page, id: string): Promise<UnitPage> {
    await page.goto(`${config.employeeUrl}/units/${id}`)
    const unitPage = new UnitPage(page)
    await unitPage.waitUntilLoaded()
    return unitPage
  }

  async navigateToUnit(id: string) {
    await this.page.goto(`${config.employeeUrl}/units/${id}/unit-info`)
  }

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="unit-attendances"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async openUnitInformation(): Promise<UnitInfoPage> {
    await this.#unitInfoTab.click()
    return new UnitInfoPage(this.page)
  }

  async openApplicationProcessTab(): Promise<ApplicationProcessPage> {
    await this.#applicationProcessTab.click()
    return new ApplicationProcessPage(this.page)
  }

  async openGroupsPage(): Promise<UnitGroupsPage> {
    await this.#groupsTab.click()
    const section = new UnitGroupsPage(this.page)
    await section.waitUntilLoaded()
    return section
  }

  async openCalendarPage(): Promise<UnitCalendarPage> {
    await this.#calendarTab.click()
    return new UnitCalendarPage(this.page)
  }

  async openWeekCalendar(groupId: UUID): Promise<UnitWeekCalendarPage> {
    const calendarPage = await this.openCalendarPage()
    await calendarPage.selectGroup(groupId)
    return await calendarPage.openWeekCalendar()
  }
}

export class UnitCalendarPage extends UnitCalendarPageBase {
  async openWeekCalendar(): Promise<UnitWeekCalendarPage> {
    await this.weekModeButton.click()
    return new UnitWeekCalendarPage(this.page)
  }
}

export class UnitInfoPage {
  #unitName: Element
  #visitingAddress: Element
  #unitDetailsLink: Element
  activeAcl: AclSection
  temporaryEmployees: TemporaryEmployeesSection
  mobileAcl: MobileDevicesSection

  constructor(private readonly page: Page) {
    this.#unitName = page.findByDataQa('unit-name')
    this.#visitingAddress = page.findByDataQa('unit-visiting-address')
    this.#unitDetailsLink = page.findByDataQa('unit-details-link')
    this.activeAcl = new AclSection(page, page.findByDataQa('unit-employees'))
    this.temporaryEmployees = new TemporaryEmployeesSection(
      page,
      page.findByDataQa('unit-employees')
    )
    this.mobileAcl = new MobileDevicesSection(
      page,
      page.findByDataQa('daycare-mobile-devices')
    )
  }

  async assertUnitName(expectedName: string) {
    await this.#unitName.assertTextEquals(expectedName)
  }

  async assertVisitingAddress(expectedAddress: string) {
    await this.#visitingAddress.assertTextEquals(expectedAddress)
  }

  async openUnitDetails(): Promise<UnitDetailsPage> {
    await this.#unitDetailsLink.click()
    const unitDetails = new UnitDetailsPage(this.page)
    await unitDetails.waitUntilLoaded()
    return unitDetails
  }
}

export class UnitDetailsPage {
  #editUnitButton: Element
  #unitName: Element
  openingAndClosingDates: Element
  #unitManagerName: Element
  #unitManagerPhone: Element
  #unitManagerEmail: Element

  constructor(private readonly page: Page) {
    this.#editUnitButton = page.findByDataQa('enable-edit-button')
    this.#unitName = page.find('[data-qa="unit-editor-container"]').find('h1')
    this.openingAndClosingDates = page.findByDataQa('opening-and-closing-dates')
    this.#unitManagerName = page.findByDataQa('unit-manager-name')
    this.#unitManagerPhone = page.findByDataQa('unit-manager-phone')
    this.#unitManagerEmail = page.findByDataQa('unit-manager-email')
  }

  async waitUntilLoaded() {
    await this.#editUnitButton.waitUntilVisible()
  }

  async assertUnitName(expectedName: string) {
    await this.#unitName.assertTextEquals(expectedName)
  }

  async assertTimeRangeByDay(dayNumber: number, expectedTime: string) {
    await this.page
      .find(`[data-qa="unit-timerange-detail-${dayNumber}"]`)
      .assertTextEquals(expectedTime)
  }

  async assertManagerData(name: string, phone: string, email: string) {
    await this.#unitManagerName.assertTextEquals(name)
    await this.#unitManagerPhone.assertTextEquals(phone)
    await this.#unitManagerEmail.assertTextEquals(email)
  }

  async edit() {
    await this.#editUnitButton.click()
    return new UnitEditor(this.page)
  }

  async openClosingDateModal() {
    await this.page.findByDataQa('open-closing-date-modal').click()
    return new UnitClosingDateModal(
      this.page.findByDataQa('unit-closing-date-modal')
    )
  }

  async assertMealTimes(mealTimes: MealTimes) {
    for (const [key, value] of Object.entries(mealTimes)) {
      await this.page
        .find(`[data-qa="${key}-value-display"]`)
        .assertTextEquals(`${value.start} - ${value.end}`)
    }
  }

  async assertShiftCareOperationTime(index: number, expected: string) {
    await this.page
      .findByDataQa(`shift-care-unit-timerange-detail-${index}`)
      .assertTextEquals(expected)
  }
}

export class UnitEditor {
  #unitNameInput: TextInput
  #areaSelect: Combobox
  providesShiftCare: Checkbox
  #managerNameInput: TextInput
  #managerPhoneInputField: TextInput
  #managerEmailInputField: TextInput
  #invoiceByMunicipality: Checkbox
  #closingDateInput: DatePicker
  #unitHandlerAddressInput: TextInput
  unitCostCenterInput: TextInput
  saveButton: Element

  constructor(private readonly page: Page) {
    this.#unitNameInput = new TextInput(page.findByDataQa('unit-name-input'))
    this.#areaSelect = new Combobox(page.findByDataQa('area-select'))
    this.providesShiftCare = new Checkbox(
      page.findByDataQa('provides-shift-care')
    )
    this.#managerNameInput = new TextInput(
      page.findByDataQa('manager-name-input')
    )
    this.#managerPhoneInputField = new TextInput(
      page.findByDataQa('qa-unit-manager-phone-input-field')
    )
    this.#managerEmailInputField = new TextInput(
      page.findByDataQa('qa-unit-manager-email-input-field')
    )
    this.#invoiceByMunicipality = new Checkbox(
      page.findByDataQa('check-invoice-by-municipality')
    )
    this.#closingDateInput = new DatePicker(
      page.findByDataQa('closing-date-input')
    )
    this.#unitHandlerAddressInput = new TextInput(
      page.find('#unit-handler-address')
    )
    this.unitCostCenterInput = new TextInput(page.find('#unit-cost-center'))
    this.saveButton = page.findByDataQa('save-button')
  }

  #timeInput(dayNumber: number, startEnd: 'start' | 'end') {
    return new TextInput(this.page.findByDataQa(`${dayNumber}-${startEnd}`))
  }

  #shiftCareTimeInput(dayNumber: number, startEnd: 'start' | 'end') {
    return new TextInput(
      this.page.findByDataQa(`shift-care-${dayNumber}-${startEnd}`)
    )
  }

  #timeCheckBox(dayNumber: number) {
    return new Checkbox(this.page.findByDataQa(`operation-day-${dayNumber}`))
  }

  #shiftCareTimeCheckBox(dayNumber: number) {
    return new Checkbox(
      this.page.findByDataQa(`shift-care-operation-day-${dayNumber}`)
    )
  }

  async fillDayTimeRange(dayNumber: number, start: string, end: string) {
    await this.#timeInput(dayNumber, 'start').fill(start)
    await this.#timeInput(dayNumber, 'end').fill(end)
  }

  async fillShiftCareDayTimeRange(
    dayNumber: number,
    start: string,
    end: string
  ) {
    await this.#shiftCareTimeInput(dayNumber, 'start').fill(start)
    await this.#shiftCareTimeInput(dayNumber, 'end').fill(end)
  }

  async clearDayTimeRange(dayNumber: number) {
    await this.#timeCheckBox(dayNumber).uncheck()
  }

  async clearShiftCareDayTimeRange(dayNumber: number) {
    await this.#shiftCareTimeCheckBox(dayNumber).uncheck()
  }

  #careTypeCheckbox(type: CareType) {
    return new Checkbox(this.page.findByDataQa(`care-type-checkbox-${type}`))
  }

  #applicationTypeCheckbox(type: ApplicationType) {
    return new Checkbox(
      this.page.findByDataQa(`application-type-checkbox-${type}`)
    )
  }

  #streetInput(type: 'visiting-address' | 'mailing-address') {
    return new TextInput(this.page.findByDataQa(`${type}-street-input`))
  }

  #postalCodeInput(type: 'visiting-address' | 'mailing-address') {
    return new TextInput(this.page.findByDataQa(`${type}-postal-code-input`))
  }

  #postOfficeInput(type: 'visiting-address' | 'mailing-address') {
    return new TextInput(this.page.findByDataQa(`${type}-post-office-input`))
  }

  readonly #providerTypeRadio = (providerType: UnitProviderType) =>
    this.page.findByDataQa(`provider-type-${providerType}`)

  static async openById(page: Page, unitId: UUID) {
    await page.goto(`${config.employeeUrl}/units/${unitId}/details`)
    await page.findByDataQa('enable-edit-button').click()

    return new UnitEditor(page)
  }

  async fillUnitName(name: string) {
    await this.#unitNameInput.fill(name)
  }

  async chooseArea(name: string) {
    await this.#areaSelect.fillAndSelectFirst(name)
  }

  async selectCareType(type: CareType) {
    await this.#careTypeCheckbox(type).check()
  }

  async toggleApplicationType(type: ApplicationType) {
    await this.#applicationTypeCheckbox(type).click()
  }

  async fillVisitingAddress(
    street: string,
    postalCode: string,
    postOffice: string
  ) {
    await this.#streetInput('visiting-address').fill(street)
    await this.#postalCodeInput('visiting-address').fill(postalCode)
    await this.#postOfficeInput('visiting-address').fill(postOffice)
  }

  async fillManagerData(name: string, phone: string, email: string) {
    await this.#managerNameInput.fill(name)
    await this.#managerPhoneInputField.fill(phone)
    await this.#managerEmailInputField.fill(email)
  }

  async setInvoiceByMunicipality(state: boolean) {
    if (state) {
      await this.#invoiceByMunicipality.check()
    } else {
      await this.#invoiceByMunicipality.uncheck()
    }
  }

  async assertWarningIsVisible(dataQa: string) {
    await this.page.findByDataQa(`${dataQa}`).waitUntilVisible()
  }

  async assertWarningIsNotVisible(dataQa: string) {
    await this.page.findByDataQa(`${dataQa}`).waitUntilHidden()
  }

  async selectClosingDate(date: LocalDate) {
    await this.#closingDateInput.fill(date)
  }

  async clearClosingDate() {
    await this.#closingDateInput.clear()
  }

  async selectProviderType(providerType: UnitProviderType) {
    await this.#providerTypeRadio(providerType).click()
  }

  async setUnitHandlerAddress(text: string) {
    await this.#unitHandlerAddressInput.fill(text)
  }

  async assertUnitHandlerAddressVisibility(
    providerType: UnitProviderType,
    handlerAddress: string,
    warningShown: boolean
  ) {
    await this.selectProviderType(providerType)
    await this.setUnitHandlerAddress(handlerAddress)
    if (warningShown) {
      await this.assertWarningIsVisible('handler-address-mandatory-warning')
    } else {
      await this.assertWarningIsNotVisible('handler-address-mandatory-warning')
    }
  }

  async submit() {
    await this.saveButton.click()
    return new UnitDetailsPage(this.page)
  }

  async fillMealTimes(mealTimes: MealTimes) {
    for (const [key, value] of Object.entries(mealTimes)) {
      const inputStart = new TextInput(
        this.page.findByDataQa(`${key}-input-start`)
      )
      await inputStart.fill(value.start)
      const inputEnd = new TextInput(this.page.findByDataQa(`${key}-input-end`))
      await inputEnd.fill(value.end)
    }
  }
}

export class UnitClosingDateModal extends Modal {
  closingDate: DatePicker
  closingDateInfo: Element

  constructor(locator: Element) {
    super(locator)
    this.closingDate = new DatePicker(this.findByDataQa('closing-date'))
    this.closingDateInfo = this.findByDataQa('closing-date-info')
  }
}

class AclModal extends Modal {
  roleCombobox = new Combobox(this.findByDataQa('role-combobox'))
  personCombobox = new Combobox(this.findByDataQa('employee-combobox'))
  groupSelect = this.findByDataQa('group-select')
  coefficientCheckbox = new Checkbox(this.findByDataQa('coefficient-checkbox'))

  async toggleGroups(groupIds: UUID[]) {
    await this.groupSelect.find('> div').click()
    for (const groupId of groupIds) {
      await this.groupSelect
        .find(`[data-qa="option"][data-id="${groupId}"]`)
        .click()
    }
    await this.groupSelect.find('> div').click()
  }
}

class TemporaryEmployeeModal extends Modal {
  employeeFirstName = new TextInput(this.findByDataQa('first-name'))
  employeeLastName = new TextInput(this.findByDataQa('last-name'))
  groupSelect = this.findByDataQa('group-select')
  pinCode = new TextInput(this.findByDataQa('pin-code'))
  coefficientCheckbox = new Checkbox(this.findByDataQa('coefficient-checkbox'))

  async toggleGroups(groupIds: UUID[]) {
    await this.groupSelect.find('> div').click()
    for (const groupId of groupIds) {
      await this.groupSelect
        .find(`[data-qa="option"][data-id="${groupId}"]`)
        .click()
    }
    await this.groupSelect.find('> div').click()
  }
}

export type AclRole =
  | 'Johtaja'
  | 'Erityisopettaja'
  | 'Varhaiskasvatussihteeri'
  | 'Henkilökunta'

class AclSection extends Element {
  constructor(
    private page: Page,
    root: Element
  ) {
    super(root)
  }

  #table = this.findByDataQa('acl-table')
  #tableRows = this.#table.findAll(`[data-qa^="acl-row-"]`)
  #tableRow = (id: UUID) => this.#table.find(`[data-qa="acl-row-${id}"]`)

  addButton = this.findByDataQa('open-add-daycare-acl-modal')
  addModal = new AclModal(this.findByDataQa('add-acl-modal'))
  editModal = new AclModal(this.findByDataQa('edit-acl-modal'))

  async addAcl(
    role: AclRole,
    email: string,
    groupIds: UUID[],
    occupancyCoefficient: boolean
  ) {
    await this.addButton.click()
    if (role !== 'Henkilökunta') {
      await this.addModal.roleCombobox.fillAndSelectFirst(role)
    }
    await this.addModal.personCombobox.fillAndSelectFirst(email)
    if (groupIds.length > 0) {
      await this.addModal.toggleGroups(groupIds)
    }
    if (occupancyCoefficient) {
      await this.addModal.coefficientCheckbox.check()
    }
    await this.addModal.submitButton.click()
    await this.#table.waitUntilVisible()
  }

  async deleteAcl(id: UUID) {
    await this.#tableRow(id).findByDataQa('delete').click()
    await new Modal(this.page.findByDataQa('confirm-delete')).submit()
  }

  async assertRowFields(
    row: Element,
    fields: {
      name: string
      email: string
      role: AclRole
      groups: string[]
      occupancyCoefficient: boolean
    }
  ) {
    await row.findByDataQa('name').assertTextEquals(fields.name)
    await row.findByDataQa('email').assertTextEquals(fields.email)
    await row.findByDataQa('role').assertTextEquals(fields.role)
    if (fields.occupancyCoefficient) {
      await row.findByDataQa('coefficient-on').waitUntilVisible()
    } else {
      await row.findByDataQa('coefficient-off').waitUntilAttached()
    }
    await waitUntilEqual(
      () => row.find('[data-qa="groups"] > div').findAll('div').allTexts(),
      fields.groups
    )
  }

  async assertRows(
    rows: {
      id: UUID
      name: string
      email: string
      role: AclRole
      groups: string[]
      occupancyCoefficient: boolean
    }[]
  ) {
    await waitUntilEqual(() => this.#tableRows.count(), rows.length)
    await Promise.all(
      rows.map((fields) =>
        this.assertRowFields(this.#tableRow(fields.id), fields)
      )
    )
  }

  getRow(id: UUID) {
    return new AclRow(this.#table.find(`[data-qa="acl-row-${id}"]`))
  }
}

class TemporaryEmployeesSection extends Element {
  constructor(
    private page: Page,
    root: Element
  ) {
    super(root)
  }

  addButton = this.findByDataQa('open-add-temporary-employee-modal')
  addModal = new TemporaryEmployeeModal(
    this.findByDataQa('add-temporary-employee-modal')
  )
  editModal = new TemporaryEmployeeModal(
    this.findByDataQa('edit-temporary-employee-modal')
  )

  #table = this.findByDataQa('temporary-employee-table')
  #tableRows = this.#table.findAll(`[data-qa^="temporary-employee-row-"]`)

  #previousTemporaryEmployeeTable = this.findByDataQa(
    'previous-temporary-employee-table'
  )
  previousTemporaryEmployeeTableRows =
    this.#previousTemporaryEmployeeTable.findAll(
      `[data-qa^="previous-temporary-employee-row-"]`
    )

  async addTemporaryAcl(
    firstName: string,
    lastName: string,
    groupIds: UUID[],
    pinCode: string
  ) {
    await this.addButton.click()
    await this.addModal.employeeFirstName.fill(firstName)
    await this.addModal.employeeLastName.fill(lastName)
    if (groupIds.length > 0) {
      await this.addModal.toggleGroups(groupIds)
    }
    await this.addModal.pinCode.fill(pinCode)
    await this.addModal.submitButton.click()
    await this.#table.waitUntilVisible()
  }

  async softDeleteTemporaryEmployeeByIndex(index: number) {
    await this.#tableRows.nth(index).findByDataQa('delete').click()
    await new Modal(this.page.findByDataQa('confirm-delete')).submit()
  }

  async hardDeleteTemporaryEmployeeByIndex(index: number) {
    await this.previousTemporaryEmployeeTableRows
      .nth(index)
      .findByDataQa('delete')
      .click()
    await new Modal(this.page.findByDataQa('confirm-delete')).submit()
  }

  async reactivateTemporaryEmployeeByIndex(index: number) {
    await this.previousTemporaryEmployeeTableRows
      .nth(index)
      .findByDataQa('reactivate')
      .click()
  }

  async assertRowFields(
    row: Element,
    fields: {
      name: string
      groups: string[]
      occupancyCoefficient: boolean
    }
  ) {
    await row.findByDataQa('name').assertTextEquals(fields.name)
    if (fields.occupancyCoefficient) {
      await row.findByDataQa('coefficient-on').waitUntilVisible()
    } else {
      await row.findByDataQa('coefficient-off').waitUntilAttached()
    }
    await waitUntilEqual(
      () => row.find('[data-qa="groups"] > div').findAll('div').allTexts(),
      fields.groups
    )
  }

  async assertRowsExactly(
    rows: {
      name: string
      groups: string[]
      occupancyCoefficient: boolean
    }[]
  ) {
    await this.#tableRows.assertCount(rows.length)
    await Promise.all(
      rows.map((fields, index) =>
        this.assertRowFields(this.#tableRows.nth(index), fields)
      )
    )
  }

  getRowByIndex(index: number) {
    return new TemporaryEmployeeRow(this.#tableRows.nth(index))
  }
}

class MobileDevicesSection extends Element {
  constructor(
    private page: Page,
    root: Element
  ) {
    super(root)
  }

  #rows = this.findAll('[data-qa="device-row"]')
  #startPairingButton = this.find('[data-qa="start-mobile-pairing"]')

  async assertDeviceExists(deviceName: string) {
    await this.#rows.find('[data-qa="name"]').assertTextEquals(deviceName)
  }

  async addMobileDevice(deviceName: string) {
    await this.#startPairingButton.click()

    const phase1 = new Modal(
      this.page.findByDataQa('mobile-pairing-modal-phase-1')
    )

    const challengeKey = await phase1.find('[data-qa="challenge-key"]').text
    const { responseKey } = await postPairingChallenge({
      body: { challengeKey }
    })
    if (!responseKey) {
      throw new Error(
        `Did not get responseKey when posting pairing challenge with key ${challengeKey}`
      )
    }

    const phase2 = new Modal(
      this.page.findByDataQa('mobile-pairing-modal-phase-2')
    )
    await new TextInput(phase2.find('[data-qa="response-key-input"]')).fill(
      responseKey
    )

    const phase3 = new Modal(
      this.page.findByDataQa('mobile-pairing-modal-phase-3')
    )
    await new TextInput(
      phase3.find('[data-qa="mobile-device-name-input"]')
    ).fill(deviceName)
    await phase3.submit()
  }
}

class AclRow extends Element {
  readonly #editButton = this.find('[data-qa="edit"]')

  async edit() {
    await this.#editButton.click()
  }
}

class TemporaryEmployeeRow extends Element {
  readonly #editButton = this.find('[data-qa="edit"]')

  async edit() {
    await this.#editButton.click()
  }
}

export class ApplicationProcessPage {
  waitingConfirmation: WaitingConfirmationSection
  placementProposals: PlacementProposalsSection
  transferApplications: TransferApplicationsSection
  serviceApplications: ServiceApplicationsSection

  constructor(private readonly page: Page) {
    this.waitingConfirmation = new WaitingConfirmationSection(
      page.findByDataQa('waiting-confirmation-section')
    )
    this.placementProposals = new PlacementProposalsSection(this.page)
    this.transferApplications = new TransferApplicationsSection(
      page.findByDataQa('transfer-applications-section')
    )
    this.serviceApplications = new ServiceApplicationsSection(this.page)
  }

  async waitUntilVisible() {
    await this.waitingConfirmation.waitUntilVisible()
  }

  async assertAbsenceApplications(expected: string[]) {
    const table = this.page.findByDataQa('absence-applications-table')
    const rows = table.findAllByDataQa('absence-application-row')
    await rows.assertTextsEqual(expected)
  }

  async openAbsenceApplication(index: number) {
    const table = this.page.findByDataQa('absence-applications-table')
    const rows = table.findAllByDataQa('absence-application-row')
    await rows.nth(index).findByDataQa('child-name').click()
    return new ChildInformationPage(this.page)
  }
}

class WaitingConfirmationSection extends Element {
  #notificationCounter = this.find('[data-qa="notification-counter"]')
  #rows = this.findAll('[data-qa="placement-plan-row"]')
  #rejectedRows = this.findAll(
    '[data-qa="placement-plan-row"][data-rejected="true"]'
  )

  async assertNotificationCounter(value: number) {
    await this.#notificationCounter.assertTextEquals(value.toString())
  }

  async assertRowCount(count: number) {
    await waitUntilEqual(() => this.#rows.count(), count)
  }

  async assertRejectedRowCount(count: number) {
    await waitUntilEqual(() => this.#rejectedRows.count(), count)
  }

  async assertRow(applicationId: string, rejected: boolean) {
    await this.find(
      `[data-qa="placement-plan-row"][data-application-id="${applicationId}"][data-rejected=${rejected.toString()}]`
    ).waitUntilVisible()
  }
}

class ServiceApplicationsSection {
  constructor(private readonly page: Page) {}

  async assertApplicationCount(n: number) {
    await this.page
      .findByDataQa('service-applications-table')
      .waitUntilVisible()
    await this.page.findAllByDataQa('service-application-row').assertCount(n)
  }

  applicationRow = (n: number) =>
    this.page.findAllByDataQa('service-application-row').nth(n)
  applicationChildLink = (n: number) =>
    this.applicationRow(n).findByDataQa('child-name')
}

class PlacementProposalsSection {
  #placementProposalTable: Element
  #acceptButton: Element

  constructor(private readonly page: Page) {
    this.#placementProposalTable = page.findByDataQa('placement-proposal-table')
    this.#acceptButton = page.findByDataQa('placement-proposals-accept-button')
  }

  #applicationRow(applicationId: string) {
    return this.page.findByDataQa(`placement-proposal-row-${applicationId}`)
  }

  async assertAcceptButtonDisabled() {
    await waitUntilTrue(() => this.#acceptButton.disabled)
  }

  async clickAcceptButton() {
    await this.#acceptButton.click()
  }

  async clickProposalAccept(applicationId: string) {
    await this.#applicationRow(applicationId)
      .find('[data-qa="accept-button"]')
      .click()
  }

  async clickProposalReject(applicationId: string) {
    await this.#applicationRow(applicationId)
      .find('[data-qa="reject-button"]')
      .click()
  }

  async selectProposalRejectionReason(n: number) {
    const radios = this.page.findAll('[data-qa="proposal-reject-reason"]')
    await radios.nth(n).click()
  }

  async submitProposalRejectionReason() {
    await this.page.findByDataQa('modal-okBtn').click()
    await this.page.findByDataQa('modal-okBtn').waitUntilHidden()
  }

  async waitUntilVisible() {
    await this.#placementProposalTable.waitUntilVisible()
  }

  async assertPlacementProposalRowCount(expected: number) {
    await this.waitUntilVisible()
    await waitUntilEqual(
      () =>
        this.#placementProposalTable.findAll('[data-qa="child-name"]').count(),
      expected
    )
  }
}

class TransferApplicationsSection extends Element {
  async assertTable(expected: TransferApplicationUnitSummary[]) {
    const rows = this.findAllByDataQa('transfer-application-row')
    await rows.assertTextsEqual(
      expected.map(
        (row) =>
          `${row.lastName} ${row.firstName}\n${row.dateOfBirth.format()}\n\t${row.preferredStartDate.format()}`
      )
    )
  }
}

export class UnitCalendarEventsSection {
  constructor(private readonly page: Page) {}

  async openDiscussionSurveyPage() {
    await this.page.findByDataQa('discussion-survey-page-button').click()
    return new DiscussionSurveyListPage(this.page)
  }

  async openEventCreationModal() {
    await this.page.findByDataQa('create-new-event-btn').click()
    return new EventCreationModal(this.page.findByDataQa('modal'))
  }

  getEventOfDay(date: LocalDate, idx: number) {
    return this.page
      .findByDataQa(`calendar-event-day-${date.formatIso()}`)
      .findAllByDataQa('event')
      .nth(idx)
  }

  getSurveyOfDay(date: LocalDate, idx: number) {
    return this.page
      .findByDataQa(`calendar-event-day-${date.formatIso()}`)
      .findAllByDataQa('survey')
      .nth(idx)
  }

  async assertNoEventsForDay(date: LocalDate) {
    await this.page
      .findByDataQa(`calendar-event-day-${date.formatIso()}`)
      .findAllByDataQa('event')
      .assertCount(0)
  }

  get eventEditModal() {
    return new EventEditModal(this.page.findByDataQa('modal'))
  }

  get surveySummaryModal() {
    return new SurveySummaryModal(this.page.findByDataQa('modal'))
  }

  get eventDeleteModal() {
    return new EventDeleteModal(
      this.page.findByDataQa('deletion-modal').findByDataQa('modal')
    )
  }
}

export class EventCreationModal extends Modal {
  readonly title = new TextInput(this.findByDataQa('title-input'))
  readonly startDateInput = new TextInput(this.findByDataQa('start-date'))
  readonly endDateInput = new TextInput(this.findByDataQa('end-date'))
  readonly description = new TextInput(this.findByDataQa('description-input'))
  readonly attendees = new TreeDropdown(this.findByDataQa('attendees'))
}

export class EventEditModal extends Modal {
  readonly title = new TextInput(this.findByDataQa('title-input'))
  readonly description = new TextInput(this.findByDataQa('description-input'))

  async submit() {
    await this.findByDataQa('save').click()
  }

  async delete() {
    await this.findByDataQa('delete').click()
  }
}

export class SurveySummaryModal extends Modal {
  async close() {
    await this.findByDataQa('close-button').click()
  }

  async assertDescription(description: string) {
    await this.findByDataQa('survey-description').assertTextEquals(description)
  }

  async assertEventTime(id: string, timeString: string) {
    await this.findByDataQa(`times-${id}`).assertTextEquals(timeString)
  }

  async assertReservee(id: string, reservee: string) {
    await this.findByDataQa(`reservee-${id}`).assertTextEquals(reservee)
  }
}

export class EventDeleteModal extends Modal {}
