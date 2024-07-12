// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { Fixture, testChild } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import ChildInformationPage from '../../pages/employee/child-information'
import EmployeeNav from '../../pages/employee/employee-nav'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let fixtures: AreaAndPersonFixtures
let page: Page
let nav: EmployeeNav
let childInfo: ChildInformationPage

beforeAll(async () => {
  await resetServiceState()
  fixtures = await initializeAreaAndPersonData()
  await Fixture.person().with(testChild).saveChild()
  await Fixture.placement()
    .with({
      childId: testChild.id,
      unitId: fixtures.testDaycare.id
    })
    .save()
})
beforeEach(async () => {
  page = await Page.open()
  nav = new EmployeeNav(page)
  childInfo = new ChildInformationPage(page)
})

describe('Child information page', () => {
  test('Admin sees every tab', async () => {
    const admin = await Fixture.employeeAdmin().save()
    await employeeLogin(page, admin)
    await page.goto(config.employeeUrl)
    await nav.tabsVisible({
      applications: true,
      units: true,
      search: true,
      finance: true,
      reports: true,
      messages: true
    })
  })

  test('Service worker sees applications, units, search and reports tabs', async () => {
    const serviceWorker = await Fixture.employeeServiceWorker().save()
    await employeeLogin(page, serviceWorker)
    await page.goto(config.employeeUrl)
    await nav.tabsVisible({
      applications: true,
      units: true,
      search: true,
      finance: false,
      reports: true,
      messages: true
    })
  })

  test('FinanceAdmin sees units, search, finance and reports tabs', async () => {
    const financeAdmin = await Fixture.employeeFinanceAdmin().save()
    await employeeLogin(page, financeAdmin)
    await page.goto(config.employeeUrl)
    await nav.tabsVisible({
      applications: false,
      units: true,
      search: true,
      finance: true,
      reports: true,
      messages: false
    })
  })

  test('Director sees only the units and reports tabs', async () => {
    const director = await Fixture.employeeDirector().save()
    await employeeLogin(page, director)
    await page.goto(config.employeeUrl)
    await nav.tabsVisible({
      applications: false,
      units: true,
      search: false,
      finance: false,
      reports: true,
      messages: false
    })
  })

  test('Reports sees only the reports tab', async () => {
    const reportViewer = await Fixture.employeeReportViewer().save()
    await employeeLogin(page, reportViewer)
    await page.goto(config.employeeUrl)
    await nav.tabsVisible({
      applications: false,
      units: false,
      search: false,
      finance: false,
      reports: true,
      messages: false
    })
  })

  test('Staff sees only the units and messaging tabs', async () => {
    const staff = await Fixture.employeeStaff(fixtures.testDaycare.id).save()
    await employeeLogin(page, staff)
    await page.goto(config.employeeUrl)
    await nav.tabsVisible({
      applications: false,
      units: true,
      search: false,
      finance: false,
      reports: false,
      messages: true
    })
  })

  test('Unit supervisor sees units, search, reports and messaging tabs', async () => {
    const unitSupervisor = await Fixture.employeeUnitSupervisor(
      fixtures.testDaycare.id
    ).save()
    await employeeLogin(page, unitSupervisor)
    await page.goto(config.employeeUrl)
    await nav.tabsVisible({
      applications: false,
      units: true,
      search: true,
      finance: false,
      reports: true,
      messages: true
    })
  })
})

describe('Child information page sections', () => {
  test('Admin sees every collapsible section', async () => {
    const admin = await Fixture.employeeAdmin().save()
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${testChild.id}`)
    await childInfo.assertCollapsiblesVisible({
      feeAlterations: true,
      guardians: true,
      placements: true,
      assistance: true,
      backupCares: true,
      familyContacts: true,
      applications: true,
      messageBlocklist: true,
      dailyServiceTimes: true,
      childDocuments: true,
      pedagogicalDocuments: true,
      income: true
    })
  })

  test('Service worker sees the correct sections', async () => {
    const serviceWorker = await Fixture.employeeServiceWorker().save()
    await employeeLogin(page, serviceWorker)
    await page.goto(`${config.employeeUrl}/child-information/${testChild.id}`)
    await childInfo.assertCollapsiblesVisible({
      feeAlterations: false,
      guardians: true,
      placements: true,
      assistance: true,
      backupCares: true,
      familyContacts: false,
      applications: true,
      messageBlocklist: true,
      dailyServiceTimes: true,
      childDocuments: false,
      pedagogicalDocuments: false,
      income: false
    })
  })

  test('Finance admin sees the correct sections', async () => {
    const financeAdmin = await Fixture.employeeFinanceAdmin().save()
    await employeeLogin(page, financeAdmin)
    await page.goto(`${config.employeeUrl}/child-information/${testChild.id}`)
    await childInfo.assertCollapsiblesVisible({
      feeAlterations: true,
      guardians: true,
      placements: true,
      assistance: false,
      backupCares: true,
      familyContacts: false,
      applications: false,
      messageBlocklist: true,
      dailyServiceTimes: true,
      childDocuments: false,
      pedagogicalDocuments: false,
      income: true
    })
  })

  test('Staff sees the correct sections', async () => {
    const staff = await Fixture.employeeStaff(fixtures.testDaycare.id).save()
    await employeeLogin(page, staff)
    await page.goto(`${config.employeeUrl}/child-information/${testChild.id}`)
    await childInfo.assertCollapsiblesVisible({
      feeAlterations: false,
      guardians: false,
      placements: true,
      assistance: true,
      backupCares: true,
      familyContacts: true,
      applications: false,
      messageBlocklist: true,
      dailyServiceTimes: true,
      childDocuments: false,
      pedagogicalDocuments: true,
      income: false
    })
  })

  test('Unit supervisor sees the correct sections', async () => {
    const unitSupervisor = await Fixture.employeeUnitSupervisor(
      fixtures.testDaycare.id
    ).save()
    await employeeLogin(page, unitSupervisor)
    await page.goto(`${config.employeeUrl}/child-information/${testChild.id}`)
    await childInfo.assertCollapsiblesVisible({
      feeAlterations: false,
      guardians: true,
      placements: true,
      assistance: true,
      backupCares: true,
      familyContacts: true,
      applications: false,
      messageBlocklist: true,
      dailyServiceTimes: true,
      childDocuments: true,
      pedagogicalDocuments: true,
      income: false
    })
  })

  test('Special education teacher sees the correct sections', async () => {
    const specialEducationTeacher =
      await Fixture.employeeSpecialEducationTeacher(
        fixtures.testDaycare.id
      ).save()
    await employeeLogin(page, specialEducationTeacher)
    await page.goto(`${config.employeeUrl}/child-information/${testChild.id}`)
    await childInfo.assertCollapsiblesVisible({
      feeAlterations: false,
      guardians: false,
      placements: true,
      assistance: true,
      backupCares: true,
      familyContacts: true,
      applications: false,
      messageBlocklist: true,
      dailyServiceTimes: true,
      childDocuments: true,
      pedagogicalDocuments: true,
      income: false
    })
  })
})
