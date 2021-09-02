// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import config from 'e2e-test-common/config'
import {
  execSimpleApplicationActions,
  insertApplications,
  resetDatabase,
  runPendingAsyncJobs
} from 'e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import { applicationFixture } from 'e2e-test-common/dev-api/fixtures'
import { enduserLogin } from 'e2e-playwright/utils/user'
import { newBrowserContext } from '../../browser'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import LocalDate from 'lib-common/local-date'

let page: Page
let header: CitizenHeader
let applicationsPage: CitizenApplicationsPage
let fixtures: AreaAndPersonFixtures

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  page = await (await newBrowserContext()).newPage()
  await page.goto(config.enduserUrl)
  await enduserLogin(page)
  header = new CitizenHeader(page)
  applicationsPage = new CitizenApplicationsPage(page)
})
afterEach(async () => {
  await page.close()
})

describe('Citizen applications list', () => {
  test('Citizen sees their children and applications', async () => {
    const application = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'PRESCHOOL',
      null,
      [fixtures.daycareFixture.id],
      true
    )
    await insertApplications([application])

    await header.applicationsTab.click()

    const child = fixtures.enduserChildFixtureJari
    await applicationsPage.assertChildIsShown(
      child.id,
      `${child.firstName} ${child.lastName}`
    )
    await applicationsPage.assertApplicationIsListed(
      application.id,
      'Esiopetushakemus',
      fixtures.daycareFixture.name,
      LocalDate.parseIso(application.form.preferredStartDate).format(),
      'Lähetetty'
    )
  })

  test('Citizen sees application that is waiting for decision acceptance', async () => {
    const application = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'DAYCARE',
      null,
      [fixtures.daycareFixture.id],
      true
    )
    await insertApplications([application])
    await execSimpleApplicationActions(application.id, [
      'move-to-waiting-placement',
      'create-default-placement-plan',
      'send-decisions-without-proposal'
    ])
    await runPendingAsyncJobs()

    await header.applicationsTab.click()
    await applicationsPage.assertApplicationIsListed(
      application.id,
      'Varhaiskasvatushakemus',
      fixtures.daycareFixture.name,
      LocalDate.parseIso(application.form.preferredStartDate).format(),
      'Vahvistettavana huoltajalla'
    )
  })

  test('Citizen can cancel a draft application', async () => {
    const application = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'DAYCARE',
      null,
      [fixtures.daycareFixture.id],
      true,
      'CREATED'
    )
    await insertApplications([application])

    await header.applicationsTab.click()
    await applicationsPage.cancelApplication(application.id)
    await applicationsPage.assertApplicationDoesNotExist(application.id)
  })

  test('Citizen can cancel a sent application', async () => {
    const application = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture,
      undefined,
      'DAYCARE',
      null,
      [fixtures.daycareFixture.id],
      true,
      'SENT'
    )
    await insertApplications([application])

    await header.applicationsTab.click()
    await applicationsPage.cancelApplication(application.id)
    await applicationsPage.assertApplicationDoesNotExist(application.id)
  })
})
