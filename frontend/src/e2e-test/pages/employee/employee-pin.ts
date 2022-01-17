// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import { Page, TextInput } from '../../utils/page'

export class EmployeePinPage {
  constructor(private readonly page: Page) {}

  readonly pinInput = new TextInput(
    this.page.find('[data-qa="pin-code-input"]')
  )
  readonly inputInfo = this.page.find('[data-qa="pin-code-input-info"]')
  readonly pinLockedAlertBox = this.page.find(
    '[data-qa="pin-locked-alert-box"]'
  )

  readonly pinSendButton = this.page.find('[data-qa="send-pin-button"]')
}
