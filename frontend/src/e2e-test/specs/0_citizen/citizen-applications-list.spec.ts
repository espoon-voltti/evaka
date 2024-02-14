// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import { execSimpleApplicationActions, insertApplications } from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { applicationFixture } from '../../dev-api/fixtures'
import { resetDatabase } from '../../generated/api-clients'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let applicationsPage: CitizenApplicationsPage
let fixtures: AreaAndPersonFixtures

const now = HelsinkiDateTime.of(2020, 1, 1, 15, 0)

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  page = await Page.open({
    mockedTime: now
  })
  await enduserLogin(page)
  header = new CitizenHeader(page)
  applicationsPage = new CitizenApplicationsPage(page)
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
    await page.reload()

    await header.selectTab('applications')

    const child = fixtures.enduserChildFixtureJari
    await applicationsPage.assertChildIsShown(
      child.id,
      `${child.firstName} ${child.lastName}`
    )
    await applicationsPage.assertApplicationIsListed(
      application.id,
      'Esiopetushakemus',
      fixtures.daycareFixture.name,
      application.form.preferences.preferredStartDate?.format() ?? '',
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
    await execSimpleApplicationActions(
      application.id,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-decisions-without-proposal'
      ],
      now
    )
    await page.reload()

    await header.selectTab('applications')
    await applicationsPage.assertApplicationIsListed(
      application.id,
      'Varhaiskasvatushakemus',
      fixtures.daycareFixture.name,
      application.form.preferences.preferredStartDate?.format() ?? '',
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
    await page.reload()

    await header.selectTab('applications')
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
    await page.reload()

    await header.selectTab('applications')
    await applicationsPage.cancelApplication(application.id)
    await applicationsPage.assertApplicationDoesNotExist(application.id)
  })
})
