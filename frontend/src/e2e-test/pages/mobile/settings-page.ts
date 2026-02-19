// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { PushNotificationCategory } from 'lib-common/generated/api-types/webpush'

import type { Page } from '../../utils/page'
import { Element, Checkbox } from '../../utils/page'

export class SettingsPage {
  languageSelection: LanguageSelection
  notificationSettings: NotificationSettings

  goBack: Element
  title: Element

  constructor(readonly page: Page) {
    this.languageSelection = new LanguageSelection(
      page.findByDataQa('language-selection')
    )
    this.notificationSettings = new NotificationSettings(
      page.findByDataQa('notification-settings')
    )

    this.goBack = page.findByDataQa('go-back')
    this.title = page.find('h1')
  }
}

export class LanguageSelection extends Element {
  fi: Element
  sv: Element

  constructor(parent: Element) {
    super(parent)
    this.fi = this.findByDataQa('lang-fi')
    this.sv = this.findByDataQa('lang-sv')
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
