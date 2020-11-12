// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { t, Selector } from 'testcafe'
import config from '../../config'

export class UnitDetailsPage {
  private readonly baseUrl = config.employeeUrl

  readonly enableEditButton = Selector('[data-qa="enable-edit-button"]')

  readonly managerPhoneInputField = Selector(
    '[data-qa="qa-unit-manager-phone-input-field"]'
  )

  readonly managerEmailInputField = Selector(
    '[data-qa="qa-unit-manager-email-input-field"]'
  )

  async enableUnitEditor() {
    await t.click(this.enableEditButton)
  }

  async fillManagerDataAndSubmitForm() {
    await t.click(this.managerPhoneInputField)
    await t.typeText(this.managerPhoneInputField, '123456789')

    await t.click(this.managerEmailInputField)
    await t.typeText(this.managerEmailInputField, 'manager@unitmanagers.fi')

    await t.pressKey('enter')
  }

  async openUnitDetailsPageById(id: string) {
    await t.navigateTo(`${this.baseUrl}/units/${id}/details`)
  }
}
