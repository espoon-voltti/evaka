// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../../config'
import { execSimpleApplicationActions } from '../../dev-api'
import {
  applicationFixture,
  familyWithDeadGuardian,
  Fixture,
  testCareArea,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createApplications,
  resetServiceState
} from '../../generated/api-clients'
import ApplicationsPage from '../../pages/employee/applications'
import EmployeeNav from '../../pages/employee/employee-nav'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let applicationsPage: ApplicationsPage

beforeEach(async () => {
  await resetServiceState()
  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family(familyWithDeadGuardian).save()
  const serviceWorker = await Fixture.employeeServiceWorker().save()

  page = await Page.open()
  applicationsPage = new ApplicationsPage(page)

  await employeeLogin(page, serviceWorker)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).applicationsTab.click()
})

describe('Applications', () => {
  test('Application with a dead applicant has to be manually sent', async () => {
    const application = applicationFixture(
      familyWithDeadGuardian.children[0],
      familyWithDeadGuardian.guardian,
      familyWithDeadGuardian.otherGuardian,
      'DAYCARE'
    )
    await createApplications({ body: [application] })
    await execSimpleApplicationActions(
      application.id,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-decisions-without-proposal'
      ],
      HelsinkiDateTime.now() // TODO: use mock clock
    )

    await applicationsPage.toggleApplicationStatusFilter('ALL')
    await applicationsPage
      .applicationRow(application.id)
      .status.assertTextEquals('Odottaa postitusta')
  })

  test('Application with a dead applicant has an indicator for the date of death', async () => {
    const application = applicationFixture(
      familyWithDeadGuardian.children[0],
      familyWithDeadGuardian.guardian,
      familyWithDeadGuardian.otherGuardian,
      'DAYCARE'
    )
    await createApplications({ body: [application] })

    await applicationsPage.toggleApplicationStatusFilter('ALL')
    const applicationDetails = await applicationsPage
      .applicationRow(application.id)
      .openApplication()
    await applicationDetails.assertApplicantIsDead()
  })
})
