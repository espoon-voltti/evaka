// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from '../../utils/page'

export default class UnreadMobileMessagesPage {
  constructor(private readonly page: Page) {}

  pinLoginButton = this.page.find(`[data-qa="pin-login-button"]`)

  linkToGroup(groupId: string) {
    return this.page.find(`[data-qa="link-to-group-messages-${groupId}"]`)
  }

  async groupLinksExist() {
    return (
      (await this.page.findAll('[data-qa^="link-to-group-messages"]').count()) >
      0
    )
  }

  async pinButtonExists() {
    return await this.pinLoginButton.waitUntilVisible()
  }
}
