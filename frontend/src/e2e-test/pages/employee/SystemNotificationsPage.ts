// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SystemNotificationTargetGroup } from 'lib-common/generated/api-types/systemnotifications'

import { DatePicker, Page, TextInput, AsyncButton } from '../../utils/page'

export class SystemNotificationsPage {
  saveButton: AsyncButton
  textInput: TextInput
  dateInput: DatePicker
  timeInput: TextInput
  constructor(private readonly page: Page) {
    this.saveButton = new AsyncButton(page.findByDataQa('save-btn'))
    this.textInput = new TextInput(page.findByDataQa('text-input'))
    this.dateInput = new DatePicker(page.findByDataQa('date-input'))
    this.timeInput = new TextInput(page.findByDataQa('time-input'))
  }

  notificationSection = (target: SystemNotificationTargetGroup) =>
    this.page.findByDataQa(`notification-${target}`)
  createButton = (target: SystemNotificationTargetGroup) =>
    this.notificationSection(target).findByDataQa('create-btn')
}
