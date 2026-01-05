// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { expect } from '@playwright/test'

import type LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'

import type { DevPerson } from '../../generated/api-types'
import type { Element, Page } from '../../utils/page'

const notificationPrefix = 'child-not-started-toast'
export default class CitizenNotificationsPage {
  constructor(private readonly page: Page) {}

  getNotificationCloseButton(notificationIndex: number) {
    const notification = this.page.findByDataQa(
      `${notificationPrefix}-${notificationIndex}`
    )
    return notification.findByDataQa('toast-close-button')
  }

  async assertNotificationIndexHidden(index: number) {
    const notification = this.page.findByDataQa(
      `${notificationPrefix}-${index}`
    )
    await notification.waitUntilHidden()
  }

  async assertStartingInfoNotificationContent(
    notificationIndex: number,
    child: DevPerson,
    daycareName: string,
    startDate: LocalDate
  ) {
    const parentElement = this.page.findByDataQa(
      `${notificationPrefix}-${notificationIndex}`
    )
    await this.assertStartingInfoContent(
      parentElement,
      child,
      daycareName,
      startDate
    )
  }

  async assertStartingInfoContent(
    parentElement: Element,
    child: DevPerson,
    daycareName: string,
    startDate: LocalDate
  ) {
    await parentElement.waitUntilVisible()
    const content = await parentElement.text

    const name = formatPersonName(child, 'FirstFirst')
    expect(content).toContain(name)
    expect(content).toContain(startDate.format())
    expect(content).toContain(daycareName)
  }
}
