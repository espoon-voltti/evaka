// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from '../../utils/page'

export default class TopNav {
  constructor(private readonly page: Page) {}

  #userMenu = this.page.find('[data-qa="top-bar-user"]')

  systemNotificationBtn = this.page.findByDataQa('system-notification-btn')
  systemNotificationModal = this.page.findByDataQa('system-notification-modal')
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
