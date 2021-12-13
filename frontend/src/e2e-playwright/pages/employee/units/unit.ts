// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Checkbox,
  Combobox,
  Element,
  Page,
  TextInput
} from 'e2e-playwright/utils/page'
import { ApplicationType } from 'lib-common/generated/api-types/application'
import { CareType } from 'lib-common/generated/api-types/daycare'
import { UUID } from 'lib-common/types'
import config from '../../../../e2e-test-common/config'
import { waitUntilEqual, waitUntilFalse, waitUntilTrue } from '../../../utils'

type UnitProviderType =
  | 'MUNICIPAL'
  | 'PURCHASED'
  | 'PRIVATE'
  | 'MUNICIPAL_SCHOOL'
  | 'PRIVATE_SERVICE_VOUCHER'
  | 'EXTERNAL_PURCHASED'

export class UnitPage {
  constructor(private readonly page: Page) {}

  readonly #unitName = this.page.find('[data-qa="unit-name"]')

  readonly #unitInfoTab = this.page.find('[data-qa="unit-info-tab"]')
  readonly #groupsTab = this.page.find('[data-qa="groups-tab"]')
  readonly #applicationProcessTab = this.page.find(
    '[data-qa="application-process-tab"]'
  )

  readonly #unitDetailsLink = this.page.find('[data-qa="unit-details-link"]')

  async navigateToUnit(id: string) {
    await this.page.goto(`${config.employeeUrl}/units/${id}`)
  }

  async assertUnitName(expectedName: string) {
    await waitUntilEqual(() => this.#unitName.innerText, expectedName)
  }

  async openUnitInformation(): Promise<UnitInformationSection> {
    await this.#unitInfoTab.click()
    const section = new UnitInformationSection(this.page)
    await section.waitUntilVisible()
    return section
  }

  async openUnitDetails(): Promise<UnitDetailsPage> {
    await this.#unitDetailsLink.click()
    const unitDetails = new UnitDetailsPage(this.page)
    await unitDetails.waitUntilLoaded()
    return unitDetails
  }

  async openApplicationProcessTab() {
    await this.#applicationProcessTab.click()
    return new ApplicationProcessPage(this.page)
  }

  async openGroups(): Promise<GroupsSection> {
    await this.#groupsTab.click()
    const section = new GroupsSection(this.page)
    await section.waitUntilVisible()
    return section
  }
}

export class UnitDetailsPage {
  constructor(private readonly page: Page) {}

  readonly #editUnitButton = this.page.find('[data-qa="enable-edit-button"]')
  readonly #unitName = this.page
    .find('[data-qa="unit-editor-container"]')
    .find('h1')

  async waitUntilLoaded() {
    await this.#editUnitButton.waitUntilVisible()
  }

  async assertUnitName(expectedName: string) {
    await waitUntilEqual(() => this.#unitName.innerText, expectedName)
  }

  readonly #unitManagerName = this.page.find('[data-qa="unit-manager-name"]')
  readonly #unitManagerPhone = this.page.find('[data-qa="unit-manager-phone"]')
  readonly #unitManagerEmail = this.page.find('[data-qa="unit-manager-email"]')

  async assertManagerData(name: string, phone: string, email: string) {
    await waitUntilEqual(() => this.#unitManagerName.innerText, name)
    await waitUntilEqual(() => this.#unitManagerPhone.innerText, phone)
    await waitUntilEqual(() => this.#unitManagerEmail.innerText, email)
  }

  async edit() {
    await this.#editUnitButton.click()
    return new UnitEditor(this.page)
  }
}

export class UnitInformationSection {
  constructor(private readonly page: Page) {}

  readonly staffAcl = new StaffAclSection(
    this.page.find('[data-qa="daycare-acl-staff"]')
  )

  async waitUntilVisible() {
    await this.page.find('[data-qa="unit-name"]').waitUntilVisible()
  }
}

export class UnitEditor {
  constructor(private readonly page: Page) {}

  readonly #unitNameInput = new TextInput(
    this.page.find('[data-qa="unit-name-input"]')
  )
  readonly #areaSelect = new Combobox(this.page.find('[data-qa="area-select"]'))

  #careTypeCheckbox(type: CareType) {
    return new Checkbox(
      this.page.find(`[data-qa="care-type-checkbox-${type}"]`)
    )
  }

  #applicationTypeCheckbox(type: ApplicationType) {
    return new Checkbox(
      this.page.find(`[data-qa="application-type-checkbox-${type}"]`)
    )
  }

  #streetInput(type: 'visiting-address' | 'mailing-address') {
    return new TextInput(this.page.find(`[data-qa="${type}-street-input"]`))
  }

  #postalCodeInput(type: 'visiting-address' | 'mailing-address') {
    return new TextInput(
      this.page.find(`[data-qa="${type}-postal-code-input"]`)
    )
  }

  #postOfficeInput(type: 'visiting-address' | 'mailing-address') {
    return new TextInput(
      this.page.find(`[data-qa="${type}-post-office-input"]`)
    )
  }

  readonly #managerNameInput = new TextInput(
    this.page.find('[data-qa="manager-name-input"]')
  )

  readonly #managerPhoneInputField = new TextInput(
    this.page.find('[data-qa="qa-unit-manager-phone-input-field"]')
  )

  readonly #managerEmailInputField = new TextInput(
    this.page.find('[data-qa="qa-unit-manager-email-input-field"]')
  )

  readonly #closingDateInput = this.page.find(
    '[data-qa="closing-date-input"] input'
  )

  readonly #reactDatePickerDays = this.page.findAll('.react-datepicker__day')

  readonly #reactDatePickerCloseIcon = this.page.find(
    '.react-datepicker__close-icon'
  )

  readonly #providerTypeRadio = (providerType: UnitProviderType) =>
    this.page.find(`[data-qa="provider-type-${providerType}"]`)

  readonly #unitHandlerAddressInput = new TextInput(
    this.page.find('#unit-handler-address')
  )

  readonly #checkInvoicedByMunicipality = this.page.find(
    '[data-qa="check-invoice-by-municipality"]'
  )
  readonly #unitCostCenterInput = new TextInput(
    this.page.find('#unit-cost-center')
  )

  readonly #invoiceByMunicipality = new Checkbox(
    this.page.find('[data-qa="check-invoice-by-municipality"]')
  )

  #submitButton = this.page.find('button[type="submit"]')

  static async openById(page: Page, unitId: UUID) {
    await page.goto(`${config.employeeUrl}/units/${unitId}/details`)
    await page.find('[data-qa="enable-edit-button"]').click()

    return new UnitEditor(page)
  }

  async fillUnitName(name: string) {
    await this.#unitNameInput.fill(name)
  }

  async chooseArea(name: string) {
    await this.#areaSelect.fillAndSelectFirst(name)
  }

  async toggleCareType(type: CareType) {
    await this.#careTypeCheckbox(type).click()
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
    await this.page.find(`[data-qa="${dataQa}"]`).waitUntilVisible()
  }

  async assertWarningIsNotVisible(dataQa: string) {
    await this.page.find(`[data-qa="${dataQa}"]`).waitUntilHidden()
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
    await this.#submitButton.click()
    return new UnitDetailsPage(this.page)
  }
}

class StaffAclSection {
  constructor(private root: Element) {}

  readonly #table = this.root.find('[data-qa="acl-table"]')
  readonly #tableRows = this.#table.findAll('[data-qa="acl-row"]')

  readonly #combobox = new Combobox(this.root.find('[data-qa="acl-combobox"]'))
  readonly #addButton = this.root.find('[data-qa="acl-add-button"]')

  async addEmployeeAcl(employeeEmail: string, employeeId: UUID) {
    await this.#combobox.click()
    await this.#combobox.fillAndSelectItem(employeeEmail, employeeId)
    await this.#addButton.click()
    await this.#table.waitUntilVisible()
  }

  get rows() {
    return this.#tableRows.evaluateAll((rows) =>
      rows.map((row) => ({
        name: row.querySelector('[data-qa="name"]')?.textContent ?? '',
        email: row.querySelector('[data-qa="email"]')?.textContent ?? '',
        groups: Array.from(
          row.querySelectorAll('[data-qa="groups"] > div')
        ).map((element) => element.textContent ?? '')
      }))
    )
  }

  async getRow(name: string): Promise<StaffAclRow> {
    const rowIndex = (await this.rows).findIndex((row) => row.name === name)
    if (rowIndex < 0) {
      throw new Error(`Row with name ${name} not found`)
    }
    return new StaffAclRow(
      this.#table.find(`tbody tr:nth-child(${rowIndex + 1})[data-qa="acl-row"]`)
    )
  }
}

class StaffAclRow {
  constructor(private root: Element) {}

  readonly #editButton = this.root.find('[data-qa="edit"]')

  async edit(): Promise<StaffAclRowEditor> {
    await this.#editButton.click()
    return new StaffAclRowEditor(this.root)
  }
}

class StaffAclRowEditor {
  constructor(private root: Element) {}

  readonly #groupEditor = this.root.find('[data-qa="groups"]')
  readonly #save = this.root.find('[data-qa="save"]')

  async toggleStaffGroups(groupIds: UUID[]) {
    await this.#groupEditor.find('> div').click()
    for (const groupId of groupIds) {
      await this.#groupEditor
        .find(`[data-qa="option"][data-id="${groupId}"]`)
        .click()
    }
    await this.#groupEditor.find('> div').click()
  }

  async save(): Promise<StaffAclRow> {
    await this.#save.click()
    return new StaffAclRow(this.root)
  }
}

class GroupsSection {
  constructor(private readonly page: Page) {}

  readonly #groupCollapsible = (groupId: string, expectIsClosed = true) =>
    this.page
      .find(
        `[data-qa="daycare-group-collapsible-${groupId}"][data-status="${
          expectIsClosed ? 'closed' : 'open'
        }"]`
      )
      .find('[data-qa="group-name"]')

  readonly #missingPlacementRow = this.page.find(
    '[data-qa="missing-placement-row"]'
  )
  readonly #terminatedPlacementRow = this.page.find(
    '[data-qa="terminated-placement-row"]'
  )

  async assertMissingPlacementRowCount(expectedCount: number) {
    await waitUntilEqual(
      () => this.#missingPlacementRow.locator.count(),
      expectedCount
    )
  }

  async assertTerminatedPlacementRowCount(expectedCount: number) {
    await waitUntilEqual(
      () => this.#terminatedPlacementRow.locator.count(),
      expectedCount
    )
  }

  async assertGroupCollapsibleIsClosed(groupId: string) {
    await this.#groupCollapsible(groupId, true).waitUntilVisible()
  }

  async assertGroupCollapsibleIsOpen(groupId: string) {
    await this.#groupCollapsible(groupId, false).waitUntilVisible()
  }

  async openGroupCollapsible(groupId: string) {
    await this.assertGroupCollapsibleIsClosed(groupId)
    await this.#groupCollapsible(groupId).click()
    await this.assertGroupCollapsibleIsOpen(groupId)
  }

  async closeGroupCollapsible(groupId: string) {
    await this.assertGroupCollapsibleIsOpen(groupId)
    await this.#groupCollapsible(groupId, false).click()
    await this.assertGroupCollapsibleIsClosed(groupId)
  }

  async waitUntilVisible() {
    await this.page
      .findAll('[data-qa="table-of-missing-groups"]')
      .nth(0)
      .waitUntilVisible()
  }
}

class ApplicationProcessPage {
  constructor(private readonly page: Page) {}

  async assertIsLoading() {
    await this.page
      .find('[data-qa="application-process-page"][data-isloading="true"]')
      .waitUntilVisible()
  }

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="application-process-page"][data-isloading="false"]')
      .waitUntilVisible()
  }

  waitingConfirmation = new WaitingConfirmationSection(
    this.page.find('[data-qa="waiting-confirmation-section"]')
  )
  placementProposals = new PlacementProposalsSection(this.page)
}

class WaitingConfirmationSection extends Element {
  #notificationCounter = this.find('[data-qa="notification-counter"]')
  #rows = this.findAll('[data-qa="placement-plan-row"]')
  #rejectedRows = this.findAll(
    '[data-qa="placement-plan-row"][data-rejected="true"]'
  )

  async assertNotificationCounter(value: number) {
    await waitUntilEqual(
      () => this.#notificationCounter.innerText,
      value.toString()
    )
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
  constructor(private readonly page: Page) {}

  #applicationRow(applicationId: string) {
    return this.page.find(`[data-qa="placement-proposal-row-${applicationId}"]`)
  }

  #acceptButton = this.page.find(
    '[data-qa="placement-proposals-accept-button"]'
  )

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
    await this.page.find('[data-qa="modal-okBtn"]').click()
  }
}
