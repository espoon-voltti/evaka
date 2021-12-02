// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Combobox, Element, Page } from 'e2e-playwright/utils/page'
import { UUID } from 'lib-common/types'

type UnitProviderType =
  | 'MUNICIPAL'
  | 'PURCHASED'
  | 'PRIVATE'
  | 'MUNICIPAL_SCHOOL'
  | 'PRIVATE_SERVICE_VOUCHER'
  | 'EXTERNAL_PURCHASED'

export class UnitPage {
  constructor(private readonly page: Page) {}

  readonly #unitInfoTab = this.page.find('[data-qa="unit-info-tab"]')
  readonly #groupsTab = this.page.find('[data-qa="groups-tab"]')

  readonly #unitDetailsLink = this.page.locator('[data-qa="unit-details-link"]')
  readonly #editUnitButton = this.page.locator('[data-qa="enable-edit-button"]')

  readonly #unitEditorContainer = this.page.locator(
    '[data-qa="unit-editor-container"]'
  )

  async openUnitInformation(): Promise<UnitInformationSection> {
    await this.#unitInfoTab.click()
    const section = new UnitInformationSection(this.page)
    await section.waitUntilVisible()
    return section
  }

  async openUnitDetails() {
    await this.#unitDetailsLink.click()
    await waitUntilVisible(this.#editUnitButton)
  }

  async clickEditUnit(): Promise<UnitEditor> {
    await this.#editUnitButton.click()
    await waitUntilVisible(this.#unitEditorContainer)
    return new UnitEditor(this.page)
  }

  async openGroups(): Promise<GroupsSection> {
    await this.#groupsTab.click()
    const section = new GroupsSection(this.page)
    await section.waitUntilVisible()
    return section
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

  readonly #closingDateInput = this.page.locator(
    '[data-qa="closing-date-input"] input'
  )

  readonly #reactDatePickerDays = this.page.locator('.react-datepicker__day')

  readonly #reactDatePickerCloseIcon = this.page.locator(
    '.react-datepicker__close-icon'
  )

  readonly #providerTypeRadio = (providerType: UnitProviderType) =>
    this.page.locator(`[data-qa="provider-type-${providerType}"]`)

  readonly #unitHandlerAddressInput = this.page.locator('#unit-handler-address')

  readonly #checkInvoicedByMunicipality = this.page.locator(
    '[data-qa="check-invoice-by-municipality"]'
  )
  readonly #unitCostCenterInput = this.page.locator('#unit-cost-center')

  async assertWarningIsVisible(dataQa: string) {
    await this.page
      .locator(`[data-qa="${dataQa}"]`)
      .waitFor({ state: 'visible' })
  }

  async assertWarningIsNotVisible(dataQa: string) {
    await this.page
      .locator(`[data-qa="${dataQa}"]`)
      .waitFor({ state: 'hidden' })
  }

  async selectSomeClosingDate() {
    await waitUntilVisible(this.#closingDateInput)
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
    await this.#unitHandlerAddressInput.selectText()
    await this.page.keyboard.press('Backspace')
    await this.#unitHandlerAddressInput.type(text)
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
    await this.#unitCostCenterInput.waitFor({
      state: visible ? 'visible' : 'hidden'
    })
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
