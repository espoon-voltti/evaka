// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import { Page, TextInput } from '../../utils/page'

export class EmployeePinPage {
  constructor(private readonly page: Page) {}

  readonly pinInput = new TextInput(this.page.findByDataQa('pin-code-input'))
  readonly inputInfo = this.page.findByDataQa('pin-code-input-info')
  readonly pinLockedAlertBox = this.page.findByDataQa('pin-locked-alert-box')

  readonly pinSendButton = this.page.findByDataQa('send-pin-button')
}
