// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Element } from '../../utils/page'

export default class TopNav {
  #userMenu: Element
  systemNotificationBtn: Element
  systemNotificationModal: Element
  constructor(private readonly page: Page) {
    this.#userMenu = page.findByDataQa('top-bar-user')
    this.systemNotificationBtn = page.findByDataQa('system-notification-btn')
    this.systemNotificationModal = page.findByDataQa(
      'system-notification-modal'
    )
  }

  systemNotificationModalClose =
    this.systemNotificationModal.findByDataQa('modal-okBtn')

  async openUserMenu() {
    await this.#userMenu.click()
  }

  async logout() {
    await this.#userMenu.find('[data-qa="logout-btn"]').click()
  }

  getUserInitials(): Promise<string> {
    return this.#userMenu.text
  }

  getFullName(): Promise<string> {
    return this.#userMenu.find('[data-qa="full-name"]').text
  }
}
