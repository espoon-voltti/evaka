// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationType } from 'lib-common/generated/api-types/application'
import { CareType } from 'lib-common/generated/api-types/daycare'
import { UUID } from 'lib-common/types'
import config from '../../../config'
import { postPairingChallenge } from '../../../dev-api'
import { waitUntilEqual, waitUntilFalse, waitUntilTrue } from '../../../utils'
import {
  Checkbox,
  Combobox,
  Element,
  Modal,
  Page,
  TextInput
} from '../../../utils/page'
import { UnitGroupsPage } from './unit-groups-page'

type UnitProviderType =
  | 'MUNICIPAL'
  | 'PURCHASED'
  | 'PRIVATE'
  | 'MUNICIPAL_SCHOOL'
  | 'PRIVATE_SERVICE_VOUCHER'
  | 'EXTERNAL_PURCHASED'

export class UnitPage {
  constructor(private readonly page: Page) {}

  #unitName = this.page.find('[data-qa="unit-name"]')
  #visitingAddress = this.page.find('[data-qa="unit-visiting-address"]')

  #unitInfoTab = this.page.find('[data-qa="unit-info-tab"]')
  #groupsTab = this.page.find('[data-qa="groups-tab"]')
  #applicationProcessTab = this.page.find('[data-qa="application-process-tab"]')

  #unitDetailsLink = this.page.find('[data-qa="unit-details-link"]')

  static async openUnit(page: Page, id: string): Promise<UnitPage> {
    await page.goto(`${config.employeeUrl}/units/${id}`)
    const unitPage = new UnitPage(page)
    await unitPage.waitUntilLoaded()
    return unitPage
  }

  async navigateToUnit(id: string) {
    await this.page.goto(`${config.employeeUrl}/units/${id}`)
  }

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="unit-information"][data-isloading="false"]')
      .waitUntilVisible()
    await this.page
      .find('[data-qa="daycare-acl"][data-isloading="false"]')
      .waitUntilVisible()
  }

  occupancies = new UnitOccupanciesSection(
    this.page.find('[data-qa="occupancies"]')
  )

  async assertUnitName(expectedName: string) {
    await waitUntilEqual(() => this.#unitName.innerText, expectedName)
  }

  async assertVisitingAddress(expectedAddress: string) {
    await waitUntilEqual(() => this.#visitingAddress.innerText, expectedAddress)
  }

  async openUnitInformation() {
    await this.#unitInfoTab.click()
  }

  supervisorAcl = new AclSection(
    this.page,
    this.page.find('[data-qa="daycare-acl-supervisors"]')
  )
  specialEducationTeacherAcl = new AclSection(
    this.page,
    this.page.find('[data-qa="daycare-acl-set"]')
  )
  staffAcl = new StaffAclSection(
    this.page,
    this.page.find('[data-qa="daycare-acl-staff"]')
  )
  mobileAcl = new MobileDevicesSection(
    this.page,
    this.page.find('[data-qa="daycare-mobile-devices"]')
  )

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

  async openGroupsPage(): Promise<UnitGroupsPage> {
    await this.#groupsTab.click()
    const section = new UnitGroupsPage(this.page)
    await section.waitUntilLoaded()
    return section
  }
}

export class UnitOccupanciesSection extends Element {
  #elem = (
    which: 'minimum' | 'maximum' | 'no-valid-values',
    type: 'confirmed' | 'planned'
  ) => this.find(`[data-qa="occupancies-${which}-${type}"]`)

  async assertNoValidValues() {
    await this.#elem('no-valid-values', 'confirmed').waitUntilVisible()
    await this.#elem('no-valid-values', 'planned').waitUntilVisible()
  }

  async assertConfirmed(minimum: string, maximum: string) {
    await waitUntilEqual(
      () => this.#elem('minimum', 'confirmed').innerText,
      minimum
    )
    await waitUntilEqual(
      () => this.#elem('maximum', 'confirmed').innerText,
      maximum
    )
  }

  async assertPlanned(minimum: string, maximum: string) {
    await waitUntilEqual(
      () => this.#elem('minimum', 'planned').innerText,
      minimum
    )
    await waitUntilEqual(
      () => this.#elem('maximum', 'planned').innerText,
      maximum
    )
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

class AclSection extends Element {
  constructor(private page: Page, root: Element) {
    super(root)
  }

  #table = this.find('[data-qa="acl-table"]')
  #tableRows = this.#table.findAll(`[data-qa^="acl-row-"]`)
  #tableRow = (id: UUID) => this.#table.find(`[data-qa="acl-row-${id}"]`)

  #combobox = new Combobox(this.find('[data-qa="acl-combobox"]'))
  #addButton = this.find('[data-qa="acl-add-button"]')

  async addAcl(email: string) {
    await this.#combobox.fillAndSelectFirst(email)
    await this.#addButton.click()
    await this.#table.waitUntilVisible()
  }

  async deleteAcl(id: UUID) {
    await this.#tableRow(id).find('[data-qa="delete"]').click()
    await new Modal(this.page.find('[data-qa="modal"]')).submit()
  }

  async assertRowFields(fields: { id: UUID; name: string; email: string }) {
    const row = this.#tableRow(fields.id)
    await waitUntilEqual(
      () => row.find('[data-qa="name"]').innerText,
      fields.name
    )
    await waitUntilEqual(
      () => row.find('[data-qa="email"]').innerText,
      fields.email
    )
  }

  async assertRows(rows: { id: UUID; name: string; email: string }[]) {
    await waitUntilEqual(() => this.#tableRows.count(), rows.length)
    await Promise.all(rows.map((fields) => this.assertRowFields(fields)))
  }
}

class StaffAclSection extends AclSection {
  #table = this.find('[data-qa="acl-table"]')
  #tableRow = (id: UUID) => this.#table.find(`[data-qa="acl-row-${id}"]`)

  async assertRowFields(fields: {
    id: UUID
    name: string
    email: string
    groups: string[]
  }) {
    const row = this.#tableRow(fields.id)
    await waitUntilEqual(
      () => row.find('[data-qa="name"]').innerText,
      fields.name
    )
    await waitUntilEqual(
      () => row.find('[data-qa="email"]').innerText,
      fields.email
    )
    await waitUntilEqual(
      () => row.find('[data-qa="groups"] > div').findAll('div').allInnerTexts(),
      fields.groups
    )
  }

  async assertRows(
    rows: { id: UUID; name: string; email: string; groups: string[] }[]
  ) {
    await Promise.all(rows.map((fields) => this.assertRowFields(fields)))
  }

  getRow(id: UUID) {
    return new StaffAclRow(this.#table.find(`[data-qa="acl-row-${id}"]`))
  }
}

class MobileDevicesSection extends Element {
  constructor(private page: Page, root: Element) {
    super(root)
  }

  #rows = this.findAll('[data-qa="device-row"]')
  #startPairingButton = this.find('[data-qa="start-mobile-pairing"]')

  async assertDeviceExists(deviceName: string) {
    await waitUntilEqual(
      () => this.#rows.find('[data-qa="name"]').innerText,
      deviceName
    )
  }

  async addMobileDevice(deviceName: string) {
    await this.#startPairingButton.click()

    const phase1 = new Modal(
      this.page.find('[data-qa="mobile-pairing-modal-phase-1"]')
    )

    const challengeKey = await phase1.find('[data-qa="challenge-key"]')
      .innerText
    const { responseKey } = await postPairingChallenge(challengeKey)
    if (!responseKey) {
      throw new Error(
        `Did not get responseKey when posting pairing challenge with key ${challengeKey}`
      )
    }

    const phase2 = new Modal(
      this.page.find('[data-qa="mobile-pairing-modal-phase-2"]')
    )
    await new TextInput(phase2.find('[data-qa="response-key-input"]')).fill(
      responseKey
    )

    const phase3 = new Modal(
      this.page.find('[data-qa="mobile-pairing-modal-phase-3"]')
    )
    await new TextInput(
      phase3.find('[data-qa="mobile-device-name-input"]')
    ).fill(deviceName)
    await phase3.submit()
  }
}

class StaffAclRow extends Element {
  readonly #editButton = this.find('[data-qa="edit"]')

  async edit(): Promise<StaffAclRowEditor> {
    await this.#editButton.click()
    return new StaffAclRowEditor(this)
  }
}

class StaffAclRowEditor extends Element {
  readonly #groupEditor = this.find('[data-qa="groups"]')
  readonly #save = this.find('[data-qa="save"]')

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
    return new StaffAclRow(this)
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
