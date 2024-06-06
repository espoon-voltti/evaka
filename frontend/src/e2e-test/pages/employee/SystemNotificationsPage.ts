// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SystemNotificationTargetGroup } from 'lib-common/generated/api-types/systemnotifications'

import { DatePicker, Page, TextInput, AsyncButton } from '../../utils/page'

export class SystemNotificationsPage {
  constructor(private readonly page: Page) {}

  notificationSection = (target: SystemNotificationTargetGroup) =>
    this.page.findByDataQa(`notification-${target}`)
  createButton = (target: SystemNotificationTargetGroup) =>
    this.notificationSection(target).findByDataQa('create-btn')
  saveButton = new AsyncButton(this.page.findByDataQa('save-btn'))
  textInput = new TextInput(this.page.findByDataQa('text-input'))
  dateInput = new DatePicker(this.page.findByDataQa('date-input'))
  timeInput = new TextInput(this.page.findByDataQa('time-input'))
}
