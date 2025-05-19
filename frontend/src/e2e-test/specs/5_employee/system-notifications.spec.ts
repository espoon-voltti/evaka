// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { SystemNotificationsPage } from '../../pages/employee/SystemNotificationsPage'
import EmployeeNav from '../../pages/employee/employee-nav'
import TopNav from '../../pages/mobile/top-nav'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let admin: DevEmployee

beforeEach(async () => {
  await resetServiceState()
  admin = await Fixture.employee().admin().save()
})

describe('System notifications', () => {
  test('notification for citizens', async () => {
    const notificationText =
      'Lakkojen takia eVakassa on nyt poikkeuksellisen paljon käyttäjiä. Jos mahdollista, kokeile palvelua myöhemmin uudelleen.'
    const notificationTextSv = 'Samma på svenska'
    const notificationTextEn = 'Same in English'
    const now = HelsinkiDateTime.of(2024, 6, 3, 11, 0)
    const validTo = HelsinkiDateTime.of(2024, 6, 3, 11, 30)

    const adminPage = await Page.open({ mockedTime: now })
    await employeeLogin(adminPage, admin)
    await adminPage.goto(config.employeeUrl)
    const nav = new EmployeeNav(adminPage)
    await nav.openAndClickDropdownMenuItem('system-notifications')

    const systemNotificationsPage = new SystemNotificationsPage(adminPage)
    await systemNotificationsPage.createButton('CITIZENS').click()
    await systemNotificationsPage.textInput.fill(notificationText)
    await systemNotificationsPage.textInputSv.fill(notificationTextSv)
    await systemNotificationsPage.textInputEn.fill(notificationTextEn)
    await systemNotificationsPage.dateInput.fill(validTo.toLocalDate().format())
    await systemNotificationsPage.timeInput.fill(validTo.toLocalTime().format())
    await systemNotificationsPage.saveButton.click()
    await systemNotificationsPage.saveButton.waitUntilHidden()
    await adminPage.close()

    const citizensPage = await Page.open({ mockedTime: now })
    await citizensPage.goto(config.enduserLoginUrl)
    await citizensPage
      .findByDataQa('system-notification')
      .assertText((t) => t.includes(notificationText))
    const header = new CitizenHeader(citizensPage)
    await header.selectLanguage('sv')
    await citizensPage
      .findByDataQa('system-notification')
      .assertText((t) => t.includes(notificationTextSv))
    await header.selectLanguage('en')
    await citizensPage
      .findByDataQa('system-notification')
      .assertText((t) => t.includes(notificationTextEn))
    await citizensPage.close()

    const citizensPage2 = await Page.open({ mockedTime: validTo.addHours(1) })
    await citizensPage2.goto(config.enduserLoginUrl)
    await citizensPage2.findByDataQa('system-notification').waitUntilHidden()
  })

  test('notification for employees', async () => {
    const notificationText = 'eVakassa on huoltokatko perjantaina klo 18 - 24.'
    const now = HelsinkiDateTime.of(2024, 6, 3, 11, 0)
    const validTo = HelsinkiDateTime.of(2024, 6, 3, 11, 30)

    const adminPage = await Page.open({ mockedTime: now })
    await employeeLogin(adminPage, admin)
    await adminPage.goto(config.employeeUrl)
    const nav = new EmployeeNav(adminPage)
    await nav.openAndClickDropdownMenuItem('system-notifications')

    const systemNotificationsPage = new SystemNotificationsPage(adminPage)
    await systemNotificationsPage.createButton('EMPLOYEES').click()
    await systemNotificationsPage.textInput.fill(notificationText)
    await systemNotificationsPage.dateInput.fill(validTo.toLocalDate().format())
    await systemNotificationsPage.timeInput.fill(validTo.toLocalTime().format())
    await systemNotificationsPage.saveButton.click()
    await systemNotificationsPage.saveButton.waitUntilHidden()
    await adminPage.close()

    // notification should be visible in employee login page
    const employeePage = await Page.open({ mockedTime: now })
    await employeePage.goto(config.employeeUrl)
    await employeePage
      .findByDataQa('system-notification')
      .assertText((s) => s.includes(notificationText))
    await employeePage.close()

    // notification should be visible in employee mobile
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({ areaId: area.id }).save()
    const mobilePage = await Page.open({ mockedTime: now })
    await mobilePage.goto(await pairMobileDevice(unit.id))
    const topBar = new TopNav(mobilePage)
    await topBar.systemNotificationBtn.click()
    await topBar.systemNotificationModal.assertText((t) =>
      t.includes(notificationText)
    )
    await topBar.systemNotificationModalClose.click()
    await topBar.systemNotificationModal.waitUntilHidden()
  })
})
