// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { t, Selector } from 'testcafe'
import config from '../../config'
import { ApplicationType } from '@evaka/lib-common/src/api-types/application/enums'

type CareType = 'DAYCARE' | 'PRESCHOOL' | 'PREPARATORY' | 'CLUB'

export class UnitDetailsPage {
  private readonly baseUrl = config.employeeUrl

  readonly createNewUnitButton = Selector('[data-qa="create-new-unit"]')

  readonly enableEditButton = Selector('[data-qa="enable-edit-button"]')

  readonly unitNameInput = Selector('[data-qa="unit-name-input"]')

  readonly careTypeCheckbox = (type: CareType) =>
    Selector(`[data-qa="care-type-checkbox-${type}"]`)

  readonly applicationTypeCheckbox = (type: ApplicationType) =>
    Selector(`[data-qa="application-type-checkbox-${type}"]`)

  readonly streetInput = (type: 'visiting-address' | 'mailing-address') =>
    Selector(`[data-qa="${type}-street-input"]`)

  readonly postalCodeInput = (type: 'visiting-address' | 'mailing-address') =>
    Selector(`[data-qa="${type}-postal-code-input"]`)

  readonly postOfficeInput = (type: 'visiting-address' | 'mailing-address') =>
    Selector(`[data-qa="${type}-post-office-input"]`)

  readonly managerNameInput = Selector('[data-qa="manager-name-input"]')

  readonly managerPhoneInputField = Selector(
    '[data-qa="qa-unit-manager-phone-input-field"]'
  )

  readonly managerEmailInputField = Selector(
    '[data-qa="qa-unit-manager-email-input-field"]'
  )

  async enableUnitEditor() {
    await t.click(this.enableEditButton)
  }

  async fillUnitName(name: string) {
    await t.click(this.unitNameInput)
    await t.typeText(this.unitNameInput, name, { replace: true })
  }

  async chooseArea(area: string) {
    const input = Selector('#react-select-2-input')
    await t.click(input)
    await t.typeText(input, area)
    await t.click(Selector('[id^="react-select-2-option-"'))
  }

  async toggleCareType(type: CareType) {
    await t.click(this.careTypeCheckbox(type))
  }

  async toggleApplicationType(type: ApplicationType) {
    await t.click(this.applicationTypeCheckbox(type))
  }

  async fillVisitingAddress(
    street: string,
    postalCode: string,
    postOffice: string
  ) {
    await t.click(this.streetInput('visiting-address'))
    await t.typeText(this.streetInput('visiting-address'), street)

    await t.click(this.postalCodeInput('visiting-address'))
    await t.typeText(this.postalCodeInput('visiting-address'), postalCode)

    await t.click(this.postOfficeInput('visiting-address'))
    await t.typeText(this.postOfficeInput('visiting-address'), postOffice)
  }

  async fillManagerData(name: string, phone: string, email: string) {
    await t.click(this.managerNameInput)
    await t.typeText(this.managerNameInput, name)

    await t.click(this.managerPhoneInputField)
    await t.typeText(this.managerPhoneInputField, phone)

    await t.click(this.managerEmailInputField)
    await t.typeText(this.managerEmailInputField, email)
  }

  async submitForm() {
    await t.pressKey('enter')
  }

  async openNewUnitEditor() {
    await this.openUnitsListPage()
    await t.click(this.createNewUnitButton)
  }

  async openUnitsListPage() {
    await t.navigateTo(`${this.baseUrl}/units`)
  }

  async openUnitDetailsPageById(id: string) {
    await t.navigateTo(`${this.baseUrl}/units/${id}/details`)
  }
}
