// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { t, Selector } from 'testcafe'

export default class MessagesPage {
  readonly unitsListUnit = (id: string) => Selector(`[data-qa="unit-${id}"]`)

  private readonly groupSelector = Selector('[data-qa="group-selector"]')

  async sendNewMessage(title: string, content: string) {
    await t.click(Selector('[data-qa="new-message-btn"]'))
    await t.typeText(Selector('[data-qa="input-title"]'), title)
    await t.typeText(Selector('[data-qa="input-content"]'), content)
    await t.click(Selector('[data-qa="select-sender"]'))
    await t.pressKey('enter')
    await t.click(Selector('[data-qa="select-receiver"]'))
    await t.pressKey('enter')
    await t.click(Selector('[data-qa="send-message-btn"]'))
  }
}
