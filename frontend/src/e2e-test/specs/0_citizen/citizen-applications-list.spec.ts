// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import { execSimpleApplicationActions } from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { applicationFixture, Fixture } from '../../dev-api/fixtures'
import {
  createApplications,
  resetServiceState
} from '../../generated/api-clients'
import CitizenApplicationsPage from '../../pages/citizen/citizen-applications'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let fixtures: AreaAndPersonFixtures

const now = HelsinkiDateTime.of(2020, 1, 1, 15, 0)

beforeEach(async () => {
  await resetServiceState()
  fixtures = await initializeAreaAndPersonData()
})

async function openApplicationsPage(citizen: { ssn: string | null }) {
  const page = await Page.open({
    mockedTime: now
  })
  await enduserLogin(page, citizen.ssn ?? undefined)
  const header = new CitizenHeader(page)
  await header.selectTab('applications')
  const applicationsPage = new CitizenApplicationsPage(page)
  return {
    page,
    header,
    applicationsPage
  }
}

describe('Citizen applications list', () => {
  test('Citizen sees their children and applications', async () => {
    const application = applicationFixture(
      fixtures.testChild,
      fixtures.testAdult,
      undefined,
      'PRESCHOOL',
      null,
      [fixtures.testDaycare.id],
      true
    )
    await createApplications({ body: [application] })
    const { applicationsPage } = await openApplicationsPage(fixtures.testAdult)

    const child = fixtures.testChild
    await applicationsPage.assertChildIsShown(
      child.id,
      `${child.firstName} ${child.lastName}`
    )
    await applicationsPage.assertApplicationIsListed(
      application.id,
      'Esiopetushakemus',
      application.form.preferences.preferredStartDate?.format() ?? '',
      'Lähetetty'
    )
  })

  test('Guardian sees their children and applications made by the other guardian', async () => {
    const child = await Fixture.person()
      .with({ ssn: '010116A9219' })
      .saveChild({ updateMockVtj: true })
    const guardian = await Fixture.person()
      .with({ ssn: '010106A973C' })
      .saveAdult({ updateMockVtjWithDependants: [child] })
    const otherGuardian = await Fixture.person()
      .with({ ssn: '010106A9388' })
      .saveAdult({ updateMockVtjWithDependants: [child] })

    const application = applicationFixture(
      child,
      guardian,
      otherGuardian,
      'PRESCHOOL',
      null,
      [fixtures.testDaycare.id],
      true
    )
    await createApplications({ body: [application] })
    const { applicationsPage } = await openApplicationsPage(otherGuardian)

    await applicationsPage.assertChildIsShown(
      child.id,
      `${child.firstName} ${child.lastName}`
    )
    await applicationsPage.assertApplicationIsListed(
      application.id,
      'Esiopetushakemus',
      application.form.preferences.preferredStartDate?.format() ?? '',
      'Lähetetty'
    )
  })

  test('Citizen sees application that is waiting for decision acceptance', async () => {
    const application = applicationFixture(
      fixtures.testChild,
      fixtures.testAdult,
      undefined,
      'DAYCARE',
      null,
      [fixtures.testDaycare.id],
      true
    )
    await createApplications({ body: [application] })
    await execSimpleApplicationActions(
      application.id,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-decisions-without-proposal'
      ],
      now
    )
    const { page, applicationsPage } = await openApplicationsPage(
      fixtures.testAdult
    )

    await page.reload()
    await applicationsPage.assertApplicationIsListed(
      application.id,
      'Varhaiskasvatushakemus',
      application.form.preferences.preferredStartDate?.format() ?? '',
      'Vahvistettavana huoltajalla'
    )
  })

  test('Citizen can cancel a draft application', async () => {
    const application = applicationFixture(
      fixtures.testChild,
      fixtures.testAdult,
      undefined,
      'DAYCARE',
      null,
      [fixtures.testDaycare.id],
      true,
      'CREATED'
    )
    await createApplications({ body: [application] })
    const { applicationsPage } = await openApplicationsPage(fixtures.testAdult)

    await applicationsPage.cancelApplication(application.id)
    await applicationsPage.assertApplicationDoesNotExist(application.id)
  })

  test('Citizen can cancel a sent application', async () => {
    const application = applicationFixture(
      fixtures.testChild,
      fixtures.testAdult,
      undefined,
      'DAYCARE',
      null,
      [fixtures.testDaycare.id],
      true,
      'SENT'
    )
    await createApplications({ body: [application] })
    const { applicationsPage } = await openApplicationsPage(fixtures.testAdult)

    await applicationsPage.cancelApplication(application.id)
    await applicationsPage.assertApplicationDoesNotExist(application.id)
  })
})
