// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Page, Element } from '../../utils/page'

export default class TopNav {
  userInitials: Element
  systemNotificationBtn: Element
  systemNotificationModal: Element
  systemNotificationModalClose: Element
  constructor(readonly page: Page) {
    this.userInitials = page.findByDataQa('top-bar-user')
    this.systemNotificationBtn = page.findByDataQa('system-notification-btn')
    this.systemNotificationModal = page.findByDataQa(
      'system-notification-modal'
    )
    this.systemNotificationModalClose =
      this.systemNotificationModal.findByDataQa('modal-okBtn')
  }

  async openUserMenu() {
    await this.userInitials.click()
  }

  async logout() {
    await this.userInitials.find('[data-qa="logout-btn"]').click()
  }

  getFullName(): Promise<string> {
    return this.userInitials.find('[data-qa="full-name"]').text
  }
}
