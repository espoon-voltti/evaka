// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Fixture } from 'e2e-test-common/dev-api/fixtures'
import { newBrowserContext } from '../../browser'
import config from 'e2e-test-common/config'
import { Page } from 'playwright'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import ChildInformationPage from 'e2e-playwright/pages/employee/child-information-page'
import {
  insertEmployeeFixture,
  resetDatabase,
  setAclForDaycares
} from 'e2e-test-common/dev-api'
import { employeeLogin } from 'e2e-playwright/utils/user'

let fixtures: AreaAndPersonFixtures
let page: Page
let nav: EmployeeNav
let childInfo: ChildInformationPage

beforeAll(async () => {
  await resetDatabase()
  ;[fixtures] = await initializeAreaAndPersonData()
  await insertEmployeeFixture({
    id: config.serviceWorkerAad,
    externalId: `espoo-ad:${config.serviceWorkerAad}`,
    email: 'paula.palveluohjaaja@espoo.fi',
    firstName: 'Paula',
    lastName: 'Palveluohjaaja',
    roles: ['SERVICE_WORKER']
  })
  await insertEmployeeFixture({
    id: config.financeAdminAad,
    externalId: `espoo-ad:${config.financeAdminAad}`,
    email: 'lasse.laskuttaja@espoo.fi',
    firstName: 'Lasse',
    lastName: 'Laskuttaja',
    roles: ['FINANCE_ADMIN']
  })
  await insertEmployeeFixture({
    id: config.directorAad,
    externalId: `espoo-ad:${config.directorAad}`,
    email: 'raisa.raportoija@espoo.fi',
    firstName: 'Raisa',
    lastName: 'Raportoija',
    roles: ['DIRECTOR']
  })
  await insertEmployeeFixture({
    id: config.unitSupervisorAad,
    externalId: `espoo-ad:${config.unitSupervisorAad}`,
    email: 'essi.esimies@espoo.fi',
    firstName: 'Essi',
    lastName: 'Esimies',
    roles: []
  })
  await setAclForDaycares(
    `espoo-ad:${config.unitSupervisorAad}`,
    fixtures.daycareFixture.id
  )
  await insertEmployeeFixture({
    id: config.staffAad,
    externalId: `espoo-ad:${config.staffAad}`,
    email: 'kaisa.kasvattaja@espoo.fi',
    firstName: 'Kaisa',
    lastName: 'Kasvattaja',
    roles: []
  })
  await setAclForDaycares(
    `espoo-ad:${config.staffAad}`,
    fixtures.daycareFixture.id,
    'STAFF'
  )
  await insertEmployeeFixture({
    id: config.specialEducationTeacher,
    externalId: `espoo-ad:${config.specialEducationTeacher}`,
    email: 'erkki.erityisopettaja@espoo.fi',
    firstName: 'Erkki',
    lastName: 'Erityisopettaja',
    roles: []
  })
  await setAclForDaycares(
    `espoo-ad:${config.specialEducationTeacher}`,
    fixtures.daycareFixture.id,
    'SPECIAL_EDUCATION_TEACHER'
  )
})
beforeEach(async () => {
  page = await (await newBrowserContext()).newPage()
  nav = new EmployeeNav(page)
  childInfo = new ChildInformationPage(page)
})
afterEach(async () => {
  await page.close()
})
afterAll(async () => {
  await Fixture.cleanup()
})

describe('Child information page', () => {
  test('Admin sees every tab', async () => {
    await employeeLogin(page, 'ADMIN')
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
    await employeeLogin(page, 'SERVICE_WORKER')
    await page.goto(config.employeeUrl)
    await nav.tabsVisible({
      applications: true,
      units: true,
      search: true,
      finance: false,
      reports: true,
      messages: false
    })
  })

  test('FinanceAdmin sees units, search, finance and reports tabs', async () => {
    await employeeLogin(page, 'FINANCE_ADMIN')
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

  test('Director sees only the reports tab', async () => {
    await employeeLogin(page, 'DIRECTOR')
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
    await employeeLogin(page, 'STAFF')
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
    await employeeLogin(page, 'UNIT_SUPERVISOR')
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
  test('Admin sees every collapsible sections', async () => {
    await employeeLogin(page, 'ADMIN')
    await page.goto(
      `${config.employeeUrl}/child-information/${fixtures.enduserChildFixtureJari.id}`
    )
    await childInfo.childCollapsiblesVisible({
      feeAlterations: true,
      guardians: true,
      fridgeParents: true,
      placements: true,
      assistance: true,
      backupCare: true,
      familyContacts: true,
      childApplications: true,
      messageBlocklist: true,
      backupPickup: true
    })
  })

  test('Service worker sees guardians, parents, placements, backup care, service need, assistance, applicaitons and family contact sections ', async () => {
    await employeeLogin(page, 'SERVICE_WORKER')
    await page.goto(
      `${config.employeeUrl}/child-information/${fixtures.enduserChildFixtureJari.id}`
    )
    await childInfo.childCollapsiblesVisible({
      feeAlterations: false,
      guardians: true,
      fridgeParents: true,
      placements: true,
      assistance: true,
      backupCare: true,
      familyContacts: true,
      childApplications: true,
      messageBlocklist: false,
      backupPickup: false
    })
  })

  test('FinanceAdmin sees fee alterations, guardians, parents, placements backup cares and service need sections', async () => {
    await employeeLogin(page, 'FINANCE_ADMIN')
    await page.goto(
      `${config.employeeUrl}/child-information/${fixtures.enduserChildFixtureJari.id}`
    )
    await childInfo.childCollapsiblesVisible({
      feeAlterations: true,
      guardians: true,
      fridgeParents: true,
      placements: true,
      assistance: false,
      backupCare: true,
      familyContacts: false,
      childApplications: false,
      messageBlocklist: false,
      backupPickup: false
    })
  })

  test('Staff sees family contacts, backup pickups, placements, backup care and service need sections', async () => {
    await employeeLogin(page, 'STAFF')
    await page.goto(
      `${config.employeeUrl}/child-information/${fixtures.enduserChildFixtureJari.id}`
    )
    await childInfo.childCollapsiblesVisible({
      feeAlterations: false,
      guardians: false,
      fridgeParents: false,
      placements: true,
      assistance: false,
      backupCare: true,
      familyContacts: true,
      childApplications: false,
      messageBlocklist: false,
      backupPickup: true
    })
  })

  test('Unit supervisor sees guardians, parents, placements, backup care, service need assistance, applications and family contacts, backup pickups sections', async () => {
    await employeeLogin(page, 'UNIT_SUPERVISOR')
    await page.goto(
      `${config.employeeUrl}/child-information/${fixtures.enduserChildFixtureJari.id}`
    )
    await childInfo.childCollapsiblesVisible({
      feeAlterations: false,
      guardians: true,
      fridgeParents: true,
      placements: true,
      assistance: true,
      backupCare: true,
      familyContacts: true,
      childApplications: true,
      messageBlocklist: false,
      backupPickup: true
    })
  })

  test('Special education techer sees family contacts, placements, backup care, service need and assistance sections', async () => {
    await employeeLogin(page, 'SPECIAL_EDUCATION_TEACHER')
    await page.goto(
      `${config.employeeUrl}/child-information/${fixtures.enduserChildFixtureJari.id}`
    )
    await childInfo.childCollapsiblesVisible({
      feeAlterations: false,
      guardians: false,
      fridgeParents: false,
      placements: true,
      assistance: true,
      backupCare: true,
      familyContacts: true,
      childApplications: false,
      messageBlocklist: false,
      backupPickup: false
    })
  })
})
