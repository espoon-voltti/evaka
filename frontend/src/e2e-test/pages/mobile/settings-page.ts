// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PushNotificationCategory } from 'lib-common/generated/api-types/webpush'

import { Page, Element, Checkbox } from '../../utils/page'

export class SettingsPage {
  notificationSettings: NotificationSettings

  goBack: Element

  constructor(readonly page: Page) {
    this.notificationSettings = new NotificationSettings(
      page.findByDataQa('notification-settings')
    )

    this.goBack = page.findByDataQa('go-back')
  }
}

export class NotificationSettings extends Element {
  permissionState: Element
  enableButton: Element

  editButton: Element
  cancelButton: Element
  saveButton: Element
  #categories: Element
  #groups: Element

  constructor(parent: Element) {
    super(parent)
    this.permissionState = this.findByDataQa('permission-state')
    this.enableButton = this.findByDataQa('enable')

    this.editButton = this.findByDataQa('edit')
    this.cancelButton = this.findByDataQa('cancel')
    this.saveButton = this.findByDataQa('save')
    this.#categories = this.findByDataQa('categories')
    this.#groups = this.findByDataQa('groups')
  }

  category(category: PushNotificationCategory): Checkbox {
    return new Checkbox(this.#categories.findByDataQa(category))
  }

  group(id: string): Checkbox {
    return new Checkbox(this.#groups.findByDataQa(id))
  }
}
