// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import {
  Fixture,
  testCareArea,
  testChild,
  testDaycare
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import ChildInformationPage from '../../pages/employee/child-information'
import EmployeeNav from '../../pages/employee/employee-nav'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let nav: EmployeeNav
let childInfo: ChildInformationPage
const area = testCareArea
const unit = testDaycare
const child = testChild
const placement = Fixture.placement({ childId: child.id, unitId: unit.id })

beforeAll(async () => {
  await resetServiceState()
  await area.save()
  await unit.save()
  await child.saveChild()
  await placement.save()
})
beforeEach(async () => {
  page = await Page.open({
    employeeCustomizations: { featureFlags: { absenceApplications: true } }
  })
  nav = new EmployeeNav(page)
  childInfo = new ChildInformationPage(page)
})

describe('Child information page', () => {
  test('Admin sees every tab', async () => {
    const admin = await Fixture.employee().admin().save()
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
    const serviceWorker = await Fixture.employee().serviceWorker().save()
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

  test('FinanceAdmin sees units, search, finance, reports and messages tabs', async () => {
    const financeAdmin = await Fixture.employee().financeAdmin().save()
    await employeeLogin(page, financeAdmin)
    await page.goto(config.employeeUrl)
    await nav.tabsVisible({
      applications: false,
      units: true,
      search: true,
      finance: true,
      reports: true,
      messages: true
    })
  })

  test('Director sees only the units and reports tabs', async () => {
    const director = await Fixture.employee().director().save()
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
    const reportViewer = await Fixture.employee().reportViewer().save()
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

  test('Staff sees only the units, reports and messaging tabs', async () => {
    const staff = await Fixture.employee().staff(unit.id).save()
    await employeeLogin(page, staff)
    await page.goto(config.employeeUrl)
    await nav.tabsVisible({
      applications: false,
      units: true,
      search: false,
      finance: false,
      reports: true,
      messages: true
    })
  })

  test('Unit supervisor sees units, search, reports and messaging tabs', async () => {
    const unitSupervisor = await Fixture.employee()
      .unitSupervisor(unit.id)
      .save()
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
    const admin = await Fixture.employee().admin().save()
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${child.id}`)
    await childInfo.assertCollapsiblesVisible({
      feeAlterations: true,
      guardians: true,
      placements: true,
      absenceApplications: true,
      serviceApplications: true,
      assistance: true,
      backupCares: true,
      familyContacts: true,
      applications: true,
      dailyServiceTimes: true,
      childDocuments: true,
      pedagogicalDocuments: true,
      income: true
    })
  })

  test('Service worker sees the correct sections', async () => {
    const serviceWorker = await Fixture.employee().serviceWorker().save()
    await employeeLogin(page, serviceWorker)
    await page.goto(`${config.employeeUrl}/child-information/${child.id}`)
    await childInfo.assertCollapsiblesVisible({
      feeAlterations: false,
      guardians: true,
      placements: true,
      absenceApplications: false,
      serviceApplications: false,
      assistance: true,
      backupCares: true,
      familyContacts: false,
      applications: true,
      dailyServiceTimes: true,
      childDocuments: false,
      pedagogicalDocuments: false,
      income: false
    })
  })

  test('Finance admin sees the correct sections', async () => {
    const financeAdmin = await Fixture.employee().financeAdmin().save()
    await employeeLogin(page, financeAdmin)
    await page.goto(`${config.employeeUrl}/child-information/${child.id}`)
    await childInfo.assertCollapsiblesVisible({
      feeAlterations: true,
      guardians: true,
      placements: true,
      absenceApplications: false,
      serviceApplications: false,
      assistance: false,
      backupCares: true,
      familyContacts: false,
      applications: false,
      dailyServiceTimes: true,
      childDocuments: false,
      pedagogicalDocuments: false,
      income: true
    })
  })

  test('Staff sees the correct sections with unit acl only', async () => {
    const staff = await Fixture.employee().staff(unit.id).save()
    await employeeLogin(page, staff)
    await page.goto(`${config.employeeUrl}/child-information/${child.id}`)
    await childInfo.assertCollapsiblesVisible({
      feeAlterations: false,
      guardians: false,
      placements: true,
      absenceApplications: false,
      serviceApplications: false,
      assistance: true,
      backupCares: true,
      familyContacts: true,
      applications: false,
      dailyServiceTimes: true,
      childDocuments: false,
      pedagogicalDocuments: true,
      income: false
    })
  })

  test('Staff sees the correct sections with group acl', async () => {
    const group = await Fixture.daycareGroup({ daycareId: unit.id }).save()
    await Fixture.groupPlacement({
      daycarePlacementId: placement.id,
      daycareGroupId: group.id,
      startDate: placement.startDate,
      endDate: placement.endDate
    }).save()
    const staff = await Fixture.employee()
      .staff(unit.id)
      .groupAcl(group.id)
      .save()
    await employeeLogin(page, staff)
    await page.goto(`${config.employeeUrl}/child-information/${child.id}`)
    await childInfo.assertCollapsiblesVisible({
      feeAlterations: false,
      guardians: false,
      placements: true,
      absenceApplications: true,
      serviceApplications: false,
      assistance: true,
      backupCares: true,
      familyContacts: true,
      applications: false,
      dailyServiceTimes: true,
      childDocuments: true,
      pedagogicalDocuments: true,
      income: false
    })
  })

  test('Unit supervisor sees the correct sections', async () => {
    const unitSupervisor = await Fixture.employee()
      .unitSupervisor(unit.id)
      .save()
    await employeeLogin(page, unitSupervisor)
    await page.goto(`${config.employeeUrl}/child-information/${child.id}`)
    await childInfo.assertCollapsiblesVisible({
      feeAlterations: false,
      guardians: true,
      placements: true,
      absenceApplications: true,
      serviceApplications: true,
      assistance: true,
      backupCares: true,
      familyContacts: true,
      applications: false,
      dailyServiceTimes: true,
      childDocuments: true,
      pedagogicalDocuments: true,
      income: false
    })
  })

  test('Special education teacher sees the correct sections', async () => {
    const specialEducationTeacher = await Fixture.employee()
      .specialEducationTeacher(unit.id)
      .save()
    await employeeLogin(page, specialEducationTeacher)
    await page.goto(`${config.employeeUrl}/child-information/${child.id}`)
    await childInfo.assertCollapsiblesVisible({
      feeAlterations: false,
      guardians: false,
      placements: true,
      absenceApplications: false,
      serviceApplications: false,
      assistance: true,
      backupCares: true,
      familyContacts: true,
      applications: false,
      dailyServiceTimes: true,
      childDocuments: true,
      pedagogicalDocuments: true,
      income: false
    })
  })
})
