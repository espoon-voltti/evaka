// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ClientFunction, Selector, t } from 'testcafe'
import { Daycare } from '../../../dev-api/types'
import { selectFirstOption } from '../../../utils/helpers'

export default class ChildInformationPage {
  readonly serviceNeedElement = (root: Selector) => ({
    root,
    serviceNeedValue: root.find('[data-qa="service-need-value"]').innerText,
    buttonRemoveServiceNeed: root.find('[data-qa="btn-remove-service-need"]'),
    buttonConfirmServiceNeedRemoval: root.find('[data-qa="modal-okBtn"]'),
    collapsible: root.find('[data-qa="collapsible-trigger"]')
  })
  readonly btnCreateNewServiceNeed: Selector = Selector(
    '[data-qa="btn-create-new-service-need"]'
  )
  readonly inputServiceNeed: Selector = Selector(
    '[data-qa="input-service-need-hours-per-week"]'
  )
  readonly btnSubmitServiceNeed: Selector = Selector(
    '[data-qa="submit-service-need-form"]'
  )

  readonly serviceNeedCollapsible = Selector(
    '[data-qa="service-need-collapsible"]'
  )

  readonly assistanceCollapsible = Selector(
    '[data-qa="assistance-collapsible"]'
  )

  readonly applicationsCollapsible = Selector(
    '[data-qa="applications-collapsible"]'
  )

  readonly btnCreateNewAssistanceNeed: Selector = Selector(
    '[data-qa="assistance-need-create-btn"]'
  )

  readonly btnConfirmAssistanceNeed: Selector = Selector(
    '[data-qa="button-assistance-need-confirm"]'
  )

  readonly inputAssistanceNeedMultiplier: Selector = Selector(
    '[data-qa="input-assistance-need-multiplier"]'
  )
  readonly backupCaresCollapsible: Selector = Selector(
    '[data-qa="backup-cares-collapsible"]'
  )

  readonly createBackupCareButton: Selector = Selector(
    '[data-qa="backup-care-create-btn"]'
  )
  readonly backupCareSelectUnit: Selector = Selector(
    '[data-qa="backup-care-select-unit"]'
  )
  readonly backupCareForm: Selector = Selector('[data-qa="backup-care-form"]')
  readonly backupCares: Selector = Selector('[data-qa="backup-cares"]')

  readonly serviceNeeds: Selector = Selector('[data-qa="service-need"]')

  readonly guardiansCollapsible: Selector = Selector(
    '[data-qa="person-guardians-collapsible"]'
  )
  readonly guardiansTable: Selector = Selector('[data-qa="table-of-guardians"]')
  readonly guardianRows: Selector = Selector('[data-qa="table-guardian-row"]')

  readonly placementsCollapsible = Selector(
    '[data-qa="child-placements-collapsible"]'
  )
  readonly newPlacementButton = this.placementsCollapsible.find('button')
  readonly placementModal = Selector('[data-qa="create-placement-modal"]')
  readonly placementModalStartDateInput = this.placementModal
    .find('.react-datepicker__input-container')
    .nth(0)
    .find('input')
  readonly placementModalEndDateInput = this.placementModal
    .find('.react-datepicker__input-container')
    .nth(1)
    .find('input')
  readonly placementModalSendButton = this.placementModal.find(
    '[data-qa="modal-okBtn"]'
  )

  readonly messageBlocklistCollapsible: Selector = Selector(
    '[data-qa="child-message-blocklist-collapsible"]'
  )

  readonly clickBlockListForParent = async (parentId: string) => {
    await t.click(
      Selector(`[data-qa="recipient-${parentId}"]`).find(
        '[data-qa="blocklist-checkbox"]'
      )
    )
  }

  async openGuardiansCollapsible() {
    await this.openCollapsible(this.guardiansCollapsible)
  }

  async openBackupCaresCollapsible() {
    await this.openCollapsible(this.backupCaresCollapsible)
  }

  async openAssistanceCollapsible() {
    await this.openCollapsible(this.assistanceCollapsible)
  }

  async openServiceNeedCollapsible() {
    await this.openCollapsible(this.serviceNeedCollapsible)
  }

  async openApplicationsCollapsible() {
    await this.openCollapsible(this.applicationsCollapsible)
  }

  async openChildMessageBlocklistCollapsible() {
    await this.openCollapsible(this.messageBlocklistCollapsible)
  }

  async openCollapsible(collapsibleSelector: Selector) {
    if ((await collapsibleSelector.getAttribute('data-status')) === 'closed') {
      await t.click(collapsibleSelector.find('[data-qa="collapsible-trigger"]'))
    }
  }

  async createBackupCare(daycare: Daycare, startDate: string, endDate: string) {
    await t.click(this.createBackupCareButton)
    await t.click(this.backupCareSelectUnit)
    await t.typeText(this.backupCareSelectUnit.find('input'), daycare.name)
    await t.click(
      this.backupCareSelectUnit.find(`[data-qa="value-${daycare.id}"]`)
    )
    const startDateInput = this.backupCareForm
      .find('[data-qa="dates"] input')
      .nth(0)
    const endDateInput = this.backupCareForm
      .find('[data-qa="dates"] input')
      .nth(1)
    await t.selectText(startDateInput).pressKey('delete')
    await t.typeText(startDateInput, startDate)
    await t.selectText(endDateInput).pressKey('delete')
    await t.typeText(endDateInput, endDate)
    await t.selectText(endDateInput).pressKey('enter')

    await t.expect(this.backupCares.visible).ok()
  }

  async getBackupCares(): Promise<Array<{ unit: string; period: string }>> {
    await t.expect(this.backupCares.visible).ok()
    return ClientFunction(() =>
      Array.from(document.querySelectorAll('[data-qa="backup-care-row"]')).map(
        (row) => ({
          unit: row.querySelector('[data-qa="unit"]')?.textContent ?? '',
          period: row.querySelector('[data-qa="period"]')?.textContent ?? ''
        })
      )
    )()
  }

  async deleteBackupCare(index: number) {
    await t.click(
      this.backupCares
        .find('[data-qa="backup-care-row"]')
        .nth(index)
        .find('[data-qa="btn-remove-backup-care"]')
    )
    const modal = this.backupCares.find('[data-qa="modal"]')
    await t.expect(modal.visible).ok()
    await t.click(this.backupCares.find('[data-qa="modal-okBtn"]'))
    await t.expect(modal.exists).notOk()
    await t.expect(this.backupCares.visible).ok()
  }

  async verifyNumberOfServiceNeeds(expected: number) {
    await t.expect(this.serviceNeeds.count).eql(expected)
  }

  async openServiceNeedForm() {
    await t.click(this.btnCreateNewServiceNeed)
  }

  async verifyServiceNeedDefaultValues() {
    await t.expect(this.inputServiceNeed.value).eql('35')
  }

  async createNewServiceNeed() {
    await t.selectText(this.inputServiceNeed).pressKey('delete')
    await t.typeText(this.inputServiceNeed, '3')
    await t.expect(this.inputServiceNeed.value).eql('30')
    await t.click(this.btnSubmitServiceNeed)
  }

  async removeServiceNeed(nth: number) {
    const serviceNeed = this.serviceNeedElement(this.serviceNeeds.nth(nth))
    await t.click(serviceNeed.buttonRemoveServiceNeed)
    await t.click(serviceNeed.buttonConfirmServiceNeedRemoval)
  }

  async verifyServiceNeedDetails(nth: number) {
    const serviceNeed = this.serviceNeedElement(this.serviceNeeds.nth(nth))
    await t.click(serviceNeed.collapsible)
    await t.expect(serviceNeed.serviceNeedValue).eql('30')
  }

  findGuardianRow(ssn: string) {
    return this.guardianRows.find('[data-qa="guardian-ssn"]').withText(ssn)
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
    if (
      (await this.placementsCollapsible.getAttribute('data-status')) ===
      'closed'
    ) {
      await t.click(this.placementsCollapsible)
    }
    await t.click(this.newPlacementButton)
    await selectFirstOption(
      this.placementModal.find('[data-qa="unit-select"]'),
      unitName
    )
    await t.typeText(this.placementModalStartDateInput, startDate, {
      replace: true
    })
    await t.click(this.placementModal.find('.react-datepicker__day--selected'))
    await t.typeText(this.placementModalEndDateInput, endDate, {
      replace: true
    })
    await t.click(this.placementModal.find('.react-datepicker__day--selected'))
    await t.click(this.placementModalSendButton)
  }

  public async openCreateApplicationModal() {
    await this.openApplicationsCollapsible()
    await t.click(Selector('[data-qa="button-create-application"]'))
  }

  public async selectGuardian(index: number) {
    const selectGuardianSelector = Selector('[data-qa="select-guardian"]')
    await t.click(selectGuardianSelector)
    await t.click(
      selectGuardianSelector.find(`[id^="react-select-2-option-${index}"`)
    )
  }

  // For selecting application guardian from vtj in application creation modal
  public async selectVtjPersonAsGuardian(ssn: string, expectedText: string) {
    const selectVtjGuardianSelector = Selector(
      '[data-qa="select-search-from-vtj-guardian"]'
    )
    await t.click(selectVtjGuardianSelector)
    await t.typeText(selectVtjGuardianSelector, ssn, {
      paste: true,
      replace: true
    })

    await t.expect(selectVtjGuardianSelector.innerText).contains(expectedText)
    await t.pressKey('enter')
  }

  public async selectCreateNewPersonAsGuardian(
    firstName: string,
    lastName: string,
    dob: string,
    streetAddress: string,
    postalCode: string,
    postOffice: string,
    phone: string,
    email: string
  ) {
    await t.click(Selector('[data-qa="radio-new-no-ssn"]'))

    await t.typeText(Selector('[data-qa="input-first-name"]'), firstName, {
      replace: true
    })

    await t.typeText(Selector('[data-qa="input-last-name"]'), lastName, {
      replace: true
    })

    const dateInput = Selector('[data-qa="datepicker-dob"]')
    await t.click(dateInput)
    await t.typeText(dateInput, dob)
    await t.pressKey('tab')
    await t.pressKey('esc')

    await t.typeText(
      Selector('[data-qa="input-street-address"]'),
      streetAddress,
      {
        replace: true
      }
    )

    await t.typeText(Selector('[data-qa="input-postal-code"]'), postalCode, {
      replace: true
    })

    await t.typeText(Selector('[data-qa="input-post-office"]'), postOffice, {
      replace: true
    })

    await t.typeText(Selector('[data-qa="input-phone"]'), phone, {
      replace: true
    })

    await t.typeText(Selector('[data-qa="input-email"]'), email, {
      replace: true
    })
  }

  public async clickCreateApplicationModalCreateApplicationButton() {
    await t.click(Selector('[data-qa="modal-okBtn"]'))
  }
}
