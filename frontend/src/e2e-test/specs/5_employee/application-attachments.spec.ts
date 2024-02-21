// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../../config'
import { execSimpleApplicationActions, insertApplications } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  applicationFixture,
  applicationFixtureId,
  daycareFixture,
  enduserChildFixtureKaarina,
  enduserGuardianFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { resetDatabase } from '../../generated/api-clients'
import ApplicationsPage from '../../pages/employee/applications'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import EmployeeNav from '../../pages/employee/employee-nav'
import { UnitPage } from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let applicationsPage: ApplicationsPage

const testFileName = 'test_file.png'
const testFilePath = `src/e2e-test/assets/${testFileName}`

beforeEach(async () => {
  await resetDatabase()
  const fixtures = await initializeAreaAndPersonData()

  const fixture = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture
  )
  await insertApplications([fixture])
  const serviceWorker = await Fixture.employeeServiceWorker().save()

  page = await Page.open()
  applicationsPage = new ApplicationsPage(page)

  await employeeLogin(page, serviceWorker.data)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).applicationsTab.click()
})

async function addAttachmentToApplication(applicationId: string) {
  const applicationView = await applicationsPage
    .applicationRow(applicationId)
    .openApplication()
  const applicationEditView = await applicationView.startEditing()
  await applicationEditView.setShiftCareNeeded()
  await applicationEditView.uploadShiftCareAttachment(testFilePath)
  await applicationEditView.assertShiftCareAttachmentUploaded(testFileName)
  await applicationEditView.saveApplication()
}

describe('Employee application attachments', () => {
  test('Employee can add and remove attachments', async () => {
    let applicationView = await applicationsPage
      .applicationRow(applicationFixtureId)
      .openApplication()
    const applicationEditView = await applicationView.startEditing()

    await applicationEditView.setUrgent()
    await applicationEditView.uploadUrgentAttachment(testFilePath)
    await applicationEditView.assertUrgentAttachmentUploaded(testFileName)

    await applicationEditView.setShiftCareNeeded()
    await applicationEditView.uploadShiftCareAttachment(testFilePath)
    await applicationEditView.assertShiftCareAttachmentUploaded(testFileName)

    await applicationEditView.deleteShiftCareAttachment(testFileName)
    await applicationEditView.assertShiftCareAttachmentsDeleted()
    await applicationEditView.saveApplication()

    applicationView = await applicationsPage
      .applicationRow(applicationFixtureId)
      .openApplication()
    await applicationView.assertUrgencyAttachmentReceivedAtVisible(testFileName)
  })

  test('Extended care attachment is visible to appropriate unit supervisor', async () => {
    await addAttachmentToApplication(applicationFixtureId)

    await execSimpleApplicationActions(
      applicationFixtureId,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-placement-proposal'
      ],
      HelsinkiDateTime.now() // TODO: use mock clock
    )

    const unitSupervisor = (
      await Fixture.employeeUnitSupervisor(daycareFixture.id).save()
    ).data

    const page2 = await Page.open()
    const unitPage = new UnitPage(page2)
    await employeeLogin(page2, unitSupervisor)
    await unitPage.navigateToUnit(daycareFixture.id)

    const view = new ApplicationReadView(page2)
    await view.navigateToApplication(applicationFixtureId)
    await view.waitUntilLoaded()
    await view.assertExtendedCareAttachmentExists(testFileName)
  })

  test('Extended care attachment is not visible to non-around-the-clock unit supervisor', async () => {
    const daycareId = uuidv4()
    await Fixture.daycare()
      .with({ ...daycareFixture, roundTheClock: false, id: daycareId })
      .save()

    const applicationId = uuidv4()
    await insertApplications([
      {
        ...applicationFixture(
          enduserChildFixtureKaarina,
          enduserGuardianFixture,
          undefined,
          'DAYCARE',
          null,
          [daycareId]
        ),
        id: applicationId
      }
    ])

    await page.reload()

    await addAttachmentToApplication(applicationId)

    await execSimpleApplicationActions(
      applicationId,
      [
        'move-to-waiting-placement',
        'create-default-placement-plan',
        'send-placement-proposal'
      ],
      HelsinkiDateTime.now() // TODO: use mock clock
    )

    const page2 = await Page.open()
    await employeeLogin(
      page2,
      (await Fixture.employeeUnitSupervisor(daycareId).save()).data
    )
    const view = new ApplicationReadView(page2)
    await view.navigateToApplication(applicationId)
    await view.waitUntilLoaded()
    await view.assertExtendedCareAttachmentDoesNotExist(testFileName)
  })
})
