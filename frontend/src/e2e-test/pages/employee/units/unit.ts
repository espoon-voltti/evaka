// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationType } from 'lib-common/generated/api-types/application'
import { CareType } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../../config'
import { postPairingChallenge } from '../../../generated/api-clients'
import { waitUntilEqual, waitUntilFalse, waitUntilTrue } from '../../../utils'
import {
  Checkbox,
  Combobox,
  Element,
  Modal,
  Page,
  SelectionChip,
  TextInput,
  TreeDropdown
} from '../../../utils/page'

import { UnitCalendarPageBase } from './unit-calendar-page-base'
import { DiscussionSurveyListPage } from './unit-discussion-survey-page'
import { UnitGroupsPage } from './unit-groups-page'
import { UnitMonthCalendarPage } from './unit-month-calendar-page'
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
  constructor(private readonly page: Page) {
    this.#unitInfoTab = page.findByDataQa('unit-info-tab')
    this.#groupsTab = page.findByDataQa('groups-tab')
    this.#calendarTab = page.findByDataQa('calendar-tab')
    this.#applicationProcessTab = page.findByDataQa('application-process-tab')
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

  async openWeekCalendar(): Promise<UnitWeekCalendarPage> {
    return await (await this.openCalendarPage()).openWeekCalendar()
  }

  async openMonthCalendar(): Promise<UnitMonthCalendarPage> {
    return await (await this.openCalendarPage()).openMonthCalendar()
  }
}

export class UnitCalendarPage extends UnitCalendarPageBase {
  async openWeekCalendar(): Promise<UnitWeekCalendarPage> {
    await this.weekModeButton.click()
    return new UnitWeekCalendarPage(this.page)
  }

  async openMonthCalendar(): Promise<UnitMonthCalendarPage> {
    await this.monthModeButton.click()
    return new UnitMonthCalendarPage(this.page)
  }
}

export class UnitInfoPage {
  #unitName: Element
  #visitingAddress: Element
  #unitDetailsLink: Element
  constructor(private readonly page: Page) {
    this.#unitName = page.findByDataQa('unit-name')
    this.#visitingAddress = page.findByDataQa('unit-visiting-address')
    this.#unitDetailsLink = page.findByDataQa('unit-details-link')
  }

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="unit-information"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async assertUnitName(expectedName: string) {
    await this.#unitName.assertTextEquals(expectedName)
  }

  async assertVisitingAddress(expectedAddress: string) {
    await this.#visitingAddress.assertTextEquals(expectedAddress)
  }

  supervisorAcl = new AclSection(
    this.page,
    this.page.findByDataQa('daycare-acl-supervisors')
  )
  specialEducationTeacherAcl = new AclSection(
    this.page,
    this.page.findByDataQa('daycare-acl-set')
  )
  earlyChildhoodEducationSecretary = new AclSection(
    this.page,
    this.page.findByDataQa('daycare-acl-eces')
  )
  staffAcl = new AclSection(
    this.page,
    this.page.findByDataQa('daycare-acl-staff')
  )
  mobileAcl = new MobileDevicesSection(
    this.page,
    this.page.findByDataQa('daycare-mobile-devices')
  )

  async openUnitDetails(): Promise<UnitDetailsPage> {
    await this.#unitDetailsLink.click()
    const unitDetails = new UnitDetailsPage(this.page)
    await unitDetails.waitUntilLoaded()
    return unitDetails
  }
}

export class UnitDetailsPage {
  #editUnitButton: Element
  #unitManagerName: Element
  #unitManagerPhone: Element
  #unitManagerEmail: Element
  constructor(private readonly page: Page) {
    this.#editUnitButton = page.findByDataQa('enable-edit-button')
    this.#unitManagerName = page.findByDataQa('unit-manager-name')
    this.#unitManagerPhone = page.findByDataQa('unit-manager-phone')
    this.#unitManagerEmail = page.findByDataQa('unit-manager-email')
  }

  readonly #unitName = this.page
    .find('[data-qa="unit-editor-container"]')
    .find('h1')

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
  #checkInvoicedByMunicipality: Element
  #invoiceByMunicipality: Checkbox
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
    this.#checkInvoicedByMunicipality = page.findByDataQa(
      'check-invoice-by-municipality'
    )
    this.#invoiceByMunicipality = new Checkbox(
      page.findByDataQa('check-invoice-by-municipality')
    )
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

  async assertShiftCareOperationChecked(dayIndex: number, checked: boolean) {
    const checkbox = new Checkbox(
      this.page.findByDataQa(`shift-care-operation-day-${dayIndex}`)
    )
    await checkbox.waitUntilChecked(checked)
  }

  async setProvidesShiftCare(on: boolean) {
    on
      ? await this.providesShiftCare.check()
      : await this.providesShiftCare.uncheck()
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

  readonly #closingDateInput = this.page.find(
    '[data-qa="closing-date-input"] input'
  )

  readonly #reactDatePickerDays = this.page.findAll('.react-datepicker__day')

  readonly #reactDatePickerCloseIcon = this.page.find(
    '.react-datepicker__close-icon'
  )

  readonly #providerTypeRadio = (providerType: UnitProviderType) =>
    this.page.findByDataQa(`provider-type-${providerType}`)

  readonly #unitHandlerAddressInput = new TextInput(
    this.page.find('#unit-handler-address')
  )

  readonly #unitCostCenterInput = new TextInput(
    this.page.find('#unit-cost-center')
  )

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

  async selectSomeClosingDate() {
    await this.#closingDateInput.waitUntilVisible()
    await this.#closingDateInput.click()
    await this.#reactDatePickerDays.nth(15).click()
  }

  async clearClosingDate() {
    await this.#reactDatePickerCloseIcon.click()
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
    warningShown
      ? await this.assertWarningIsVisible('handler-address-mandatory-warning')
      : await this.assertWarningIsNotVisible(
          'handler-address-mandatory-warning'
        )
  }

  async clickInvoicedByMunicipality() {
    await this.#checkInvoicedByMunicipality.click()
  }

  async assertInvoicingFieldsVisibility(visible: boolean) {
    visible
      ? await this.#unitCostCenterInput.waitUntilVisible()
      : await this.#unitCostCenterInput.waitUntilHidden()
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

class DaycareAclAdditionModal extends Modal {
  permanentSelect = new SelectionChip(
    this.find('[data-qa="add-daycare-acl-type-permanent"]')
  )
  temporarySelect = new SelectionChip(
    this.find('[data-qa="add-daycare-acl-type-temporary"]')
  )
  personCombobox = new Combobox(
    this.find('[data-qa="add-daycare-acl-emp-combobox"]')
  )
  employeeFirstName = new TextInput(
    this.find('[data-qa="add-daycare-acl-first-name"]')
  )
  employeeLastName = new TextInput(
    this.find('[data-qa="add-daycare-acl-last-name"]')
  )
  groupSelect = this.find('[data-qa="add-daycare-acl-group-select"]')
  pinCode = new TextInput(this.find('[data-qa="add-daycare-acl-pin-code"]'))
  submitButton = this.find('[data-qa="add-daycare-acl-save-btn"]')
  coefficientCheckbox = new Checkbox(
    this.find('[data-qa="add-daycare-acl-coeff-checkbox"]')
  )

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

export class EmployeeRowEditModal extends Modal {
  firstName = new TextInput(this.find('[data-qa="first-name"]'))
  lastName = new TextInput(this.find('[data-qa="last-name"]'))
  groupSelect = this.find('[data-qa="group-select"]')
  saveButton = this.find('[data-qa="edit-acl-row-save-btn"]')
  coefficientCheckbox = new Checkbox(
    this.find('[data-qa="edit-acl-modal-coeff-checkbox"]')
  )
  pinCode = new TextInput(this.find('[data-qa="pin-code"]'))

  async setFirstName(firstName: string) {
    await this.firstName.waitUntilVisible()
    await this.firstName.fill(firstName)
  }

  async setLastName(lastName: string) {
    await this.lastName.waitUntilVisible()
    await this.lastName.fill(lastName)
  }

  async toggleGroups(groupIds: UUID[]) {
    await this.groupSelect.find('> div').click()
    for (const groupId of groupIds) {
      await this.groupSelect
        .find(`[data-qa="option"][data-id="${groupId}"]`)
        .click()
    }
    await this.groupSelect.find('> div').click()
  }

  async setCoefficient(value: boolean) {
    await this.coefficientCheckbox.waitUntilVisible()
    if (value) {
      await this.coefficientCheckbox.check()
    } else {
      await this.coefficientCheckbox.uncheck()
    }
  }

  async setPinCode(pinCode: string) {
    await this.pinCode.waitUntilVisible()
    await this.pinCode.fill(pinCode)
  }

  async assertPinCode(pinCode: string) {
    await this.pinCode.assertValueEquals(pinCode)
  }

  async submitWithTemporaryEmployee({
    firstName,
    lastName,
    toggleGroups,
    coefficient,
    pinCode
  }: {
    firstName?: string
    lastName?: string
    toggleGroups?: UUID[]
    coefficient?: boolean
    pinCode?: string
  }) {
    if (firstName !== undefined) {
      await this.setFirstName(firstName)
    }
    if (lastName !== undefined) {
      await this.setLastName(lastName)
    }
    if (toggleGroups !== undefined) {
      await this.toggleGroups(toggleGroups)
    }
    if (coefficient !== undefined) {
      await this.setCoefficient(coefficient)
    }
    if (pinCode !== undefined) {
      await this.setPinCode(pinCode)
    }
    await this.saveButton.click()
  }
}

class AclSection extends Element {
  constructor(
    private page: Page,
    root: Element
  ) {
    super(root)
  }

  #table = this.find('[data-qa="acl-table"]')
  #tableRows = this.#table.findAll(`[data-qa^="acl-row-"]`)
  #tableRow = (id: UUID) => this.#table.find(`[data-qa="acl-row-${id}"]`)

  #previousTemporaryEmployeeTable = this.find(
    '[data-qa="previous-temporary-employee-table"]'
  )
  #previousTemporaryEmployeeTableRows =
    this.#previousTemporaryEmployeeTable.findAll(`[data-qa^="acl-row-"]`)

  #addButton = this.find('[data-qa="open-add-daycare-acl-modal"]')

  async assertTemporaryEmployeeHidden() {
    await this.#addButton.click()
    const addModal = new DaycareAclAdditionModal(
      this.page.findByDataQa('add-daycare-acl-modal')
    )
    await addModal.temporarySelect.waitUntilHidden()
  }

  async assertTemporaryEmployeeVisible() {
    await this.#addButton.click()
    const addModal = new DaycareAclAdditionModal(
      this.page.findByDataQa('add-daycare-acl-modal')
    )
    await addModal.temporarySelect.waitUntilVisible()
  }

  async addAcl(email: string, groupIds: UUID[], occupancyCoefficient: boolean) {
    await this.#addButton.click()
    const addModal = new DaycareAclAdditionModal(
      this.page.findByDataQa('add-daycare-acl-modal')
    )
    await addModal.personCombobox.fillAndSelectFirst(email)
    if (groupIds.length > 0) {
      await addModal.toggleGroups(groupIds)
    }
    if (occupancyCoefficient) {
      await addModal.coefficientCheckbox.check()
    }
    await addModal.submitButton.click()
    await this.#table.waitUntilVisible()
  }

  async addTemporaryAcl(
    firstName: string,
    lastName: string,
    groupIds: UUID[],
    pinCode: string
  ) {
    await this.#addButton.click()
    const addModal = new DaycareAclAdditionModal(
      this.page.findByDataQa('add-daycare-acl-modal')
    )
    await addModal.temporarySelect.check()
    await addModal.employeeFirstName.fill(firstName)
    await addModal.employeeLastName.fill(lastName)
    if (groupIds.length > 0) {
      await addModal.toggleGroups(groupIds)
    }
    await addModal.pinCode.fill(pinCode)
    await addModal.submitButton.click()
    await this.#table.waitUntilVisible()
  }

  async deleteAcl(id: UUID) {
    await this.#tableRow(id).find('[data-qa="delete"]').click()
    await new Modal(this.page.findByDataQa('remove-daycare-acl-modal')).submit()
  }

  async deleteAclByIndex(index: number) {
    await this.#tableRows.nth(index).find('[data-qa="delete"]').click()
    await new Modal(this.page.findByDataQa('remove-daycare-acl-modal')).submit()
  }

  async deleteTemporaryEmployeeByIndex(index: number) {
    await this.#previousTemporaryEmployeeTableRows
      .nth(index)
      .find('[data-qa="delete"]')
      .click()
    await new Modal(
      this.page.findByDataQa('remove-temporary-employee-modal')
    ).submit()
  }

  async assertRowFields(
    row: Element,
    fields: {
      name: string
      email: string
      groups: string[]
      occupancyCoefficient: boolean
    }
  ) {
    await row
      .find('[data-qa="name"] > [data-qa="text"]')
      .assertTextEquals(fields.name)
    await row.find('[data-qa="email"]').assertTextEquals(fields.email)
    await row
      .find(
        `[data-qa="coefficient-${fields.occupancyCoefficient ? 'on' : 'off'}"]`
      )
      .waitUntilVisible()
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

  async assertRowsExactly(
    rows: {
      name: string
      email: string
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

  async assertPreviousTemporaryEmployeeRowsExactly(
    rows: {
      name: string
      email: string
      groups: string[]
      occupancyCoefficient: boolean
    }[]
  ) {
    await this.#previousTemporaryEmployeeTableRows.assertCount(rows.length)
    await Promise.all(
      rows.map((fields, index) =>
        this.assertRowFields(
          this.#previousTemporaryEmployeeTableRows.nth(index),
          fields
        )
      )
    )
  }

  getRow(id: UUID) {
    return new AclRow(this.#table.find(`[data-qa="acl-row-${id}"]`))
  }

  getRowByIndex(index: number) {
    return new AclRow(this.#tableRows.nth(index))
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

export class ApplicationProcessPage {
  waitingConfirmation: WaitingConfirmationSection
  constructor(private readonly page: Page) {
    this.waitingConfirmation = new WaitingConfirmationSection(
      page.findByDataQa('waiting-confirmation-section')
    )
  }

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="application-process-page"][data-isloading="false"]')
      .waitUntilVisible()
  }

  placementProposals = new PlacementProposalsSection(this.page)

  async waitUntilVisible() {
    await this.waitingConfirmation.waitUntilVisible()
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

  async assertAcceptButtonEnabled() {
    await waitUntilFalse(() => this.#acceptButton.disabled)
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

  async assertNoEventsForDay(date: LocalDate) {
    await this.page
      .findByDataQa(`calendar-event-day-${date.formatIso()}`)
      .findAllByDataQa('event')
      .assertCount(0)
  }

  get eventEditModal() {
    return new EventEditModal(this.page.findByDataQa('modal'))
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

export class EventDeleteModal extends Modal {}
