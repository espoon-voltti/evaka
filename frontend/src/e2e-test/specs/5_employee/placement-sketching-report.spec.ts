// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  applicationFixture,
  createDaycarePlacementFixture,
  testDaycare,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createApplications,
  createDaycarePlacements,
  resetServiceState
} from '../../generated/api-clients'
import { DevApplicationWithForm } from '../../generated/api-types'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let fixtures: AreaAndPersonFixtures
let page: Page

const mockToday = LocalDate.of(2021, 2, 1)

beforeEach(async () => {
  await resetServiceState()
  fixtures = await initializeAreaAndPersonData()
  const admin = await Fixture.employeeAdmin().save()

  page = await Page.open({
    mockedTime: mockToday.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  await employeeLogin(page, admin)
})

async function openPlacementSketchingReport() {
  await page.goto(config.employeeUrl + '/reports')
  const reports = new ReportsPage(page)
  return await reports.openPlacementSketchingReport()
}

describe('Placement sketching report', () => {
  test('Not placed child shows on report', async () => {
    const fixture = applicationFixture(
      fixtures.testChild,
      fixtures.testAdult,
      undefined,
      'PRESCHOOL',
      'AGREED'
    )

    const preferredStartDate = LocalDate.of(2021, 8, 13)
    const sentDate = preferredStartDate.subMonths(4)
    const applicationId = uuidv4()

    const createdApplication: DevApplicationWithForm = {
      ...fixture,
      form: {
        ...fixture.form,
        preferences: {
          ...fixture.form.preferences,
          preferredStartDate
        }
      },
      sentDate: sentDate,
      status: 'SENT',
      id: applicationId
    }

    await createApplications({ body: [createdApplication] })

    const preferredUnit = testDaycare

    const report = await openPlacementSketchingReport()
    await report.assertRow(
      createdApplication.id,
      preferredUnit.name,
      `${fixtures.testChild.lastName} ${fixtures.testChild.firstName}`
    )
  })

  test('Placed child shows on report', async () => {
    const fixture = applicationFixture(
      fixtures.testChild,
      fixtures.testAdult,
      undefined,
      'PRESCHOOL',
      'AGREED'
    )

    const preferredStartDate = LocalDate.of(2021, 8, 13)
    const sentDate = preferredStartDate.subMonths(4)
    const applicationId = uuidv4()

    const createdApplication: DevApplicationWithForm = {
      ...fixture,
      form: {
        ...fixture.form,
        preferences: {
          ...fixture.form.preferences,
          preferredStartDate
        }
      },
      sentDate: sentDate,
      status: 'SENT',
      id: applicationId
    }

    await createApplications({ body: [createdApplication] })

    const placementStartDate = LocalDate.of(2021, 1, 1)
    const preferredUnit = testDaycare
    const currentUnit = preferredUnit

    const daycarePlacementFixture = createDaycarePlacementFixture(
      uuidv4(),
      createdApplication.childId,
      preferredUnit.id,
      placementStartDate
    )
    await createDaycarePlacements({ body: [daycarePlacementFixture] })

    const report = await openPlacementSketchingReport()
    await report.assertRow(
      createdApplication.id,
      preferredUnit.name,
      `${fixtures.testChild.lastName} ${fixtures.testChild.firstName}`,
      currentUnit.name
    )
  })

  test('Application status filter works', async () => {
    const preferredStartDate = LocalDate.of(2021, 8, 13)
    const sentDate = preferredStartDate.subMonths(4)

    const fixtureForStatusSent = applicationFixture(
      fixtures.testChild,
      fixtures.testAdult,
      undefined,
      'PRESCHOOL',
      'AGREED'
    )
    const applicationWithStatusSent: DevApplicationWithForm = {
      ...fixtureForStatusSent,
      form: {
        ...fixtureForStatusSent.form,
        preferences: {
          ...fixtureForStatusSent.form.preferences,
          preferredStartDate
        }
      },
      sentDate: sentDate,
      status: 'SENT',
      id: uuidv4()
    }

    const fixtureForStatusWaitingPlacement = applicationFixture(
      fixtures.testChild2,
      fixtures.testAdult,
      undefined,
      'PRESCHOOL',
      'AGREED'
    )
    const applicationWithStatusWaitingPlacement: DevApplicationWithForm = {
      ...fixtureForStatusWaitingPlacement,
      form: {
        ...fixtureForStatusWaitingPlacement.form,
        preferences: {
          ...fixtureForStatusWaitingPlacement.form.preferences,
          preferredStartDate
        }
      },
      sentDate: sentDate,
      status: 'WAITING_PLACEMENT',
      id: uuidv4()
    }

    const fixtureForStatusActive = applicationFixture(
      fixtures.testChildRestricted,
      fixtures.testAdult,
      undefined,
      'PRESCHOOL',
      'AGREED'
    )
    const applicationWithStatusActive: DevApplicationWithForm = {
      ...fixtureForStatusActive,
      form: {
        ...fixtureForStatusActive.form,
        preferences: {
          ...fixtureForStatusActive.form.preferences,
          preferredStartDate
        }
      },
      sentDate: sentDate,
      status: 'ACTIVE',
      id: uuidv4()
    }

    await createApplications({
      body: [
        applicationWithStatusSent,
        applicationWithStatusWaitingPlacement,
        applicationWithStatusActive
      ]
    })

    const report = await openPlacementSketchingReport()
    await report.assertRow(
      applicationWithStatusSent.id,
      testDaycare.name,
      `${fixtures.testChild.lastName} ${fixtures.testChild.firstName}`
    )
    await report.assertRow(
      applicationWithStatusWaitingPlacement.id,
      testDaycare.name,
      `${fixtures.testChild2.lastName} ${fixtures.testChild2.firstName}`
    )
    await report.assertRow(
      applicationWithStatusActive.id,
      testDaycare.name,
      `${fixtures.testChildRestricted.lastName} ${fixtures.testChildRestricted.firstName}`
    )

    await report.toggleApplicationStatus('SENT')
    await report.assertRow(
      applicationWithStatusSent.id,
      testDaycare.name,
      `${fixtures.testChild.lastName} ${fixtures.testChild.firstName}`
    )
    await report.assertNotRow(applicationWithStatusWaitingPlacement.id)
    await report.assertNotRow(applicationWithStatusActive.id)

    await report.toggleApplicationStatus('WAITING_PLACEMENT')
    await report.assertRow(
      applicationWithStatusSent.id,
      testDaycare.name,
      `${fixtures.testChild.lastName} ${fixtures.testChild.firstName}`
    )
    await report.assertRow(
      applicationWithStatusWaitingPlacement.id,
      testDaycare.name,
      `${fixtures.testChild2.lastName} ${fixtures.testChild2.firstName}`
    )
    await report.assertNotRow(applicationWithStatusActive.id)

    await report.toggleApplicationStatus('SENT')
    await report.assertNotRow(applicationWithStatusSent.id)
    await report.assertRow(
      applicationWithStatusWaitingPlacement.id,
      testDaycare.name,
      `${fixtures.testChild2.lastName} ${fixtures.testChild2.firstName}`
    )
    await report.assertNotRow(applicationWithStatusActive.id)
  })
})
