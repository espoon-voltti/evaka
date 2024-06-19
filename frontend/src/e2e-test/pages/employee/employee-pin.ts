// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import { Page, TextInput, Element } from '../../utils/page'

export class EmployeePinPage {
  pinInput: TextInput
  inputInfo: Element
  pinLockedAlertBox: Element
  pinSendButton: Element
  constructor(readonly page: Page) {
    this.pinInput = new TextInput(page.findByDataQa('pin-code-input'))
    this.inputInfo = page.findByDataQa('pin-code-input-info')
    this.pinLockedAlertBox = page.findByDataQa('pin-locked-alert-box')
    this.pinSendButton = page.findByDataQa('send-pin-button')
  }
}
