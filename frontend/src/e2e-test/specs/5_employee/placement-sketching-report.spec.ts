// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  ApplicationId,
  PlacementId
} from 'lib-common/generated/api-types/shared'
import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import {
  applicationFixture,
  createDaycarePlacementFixture,
  testDaycare,
  Fixture,
  testAdult,
  testChild,
  testChild2,
  testChildRestricted,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createApplications,
  createDaycarePlacements,
  resetServiceState
} from '../../generated/api-clients'
import type { DevApplicationWithForm } from '../../generated/api-types'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page

const mockToday = LocalDate.of(2021, 2, 1)

beforeEach(async () => {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2, testChildRestricted]
  }).save()
  const admin = await Fixture.employee().admin().save()

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
      testChild,
      testAdult,
      undefined,
      'PRESCHOOL',
      'AGREED'
    )

    const preferredStartDate = LocalDate.of(2021, 8, 13)
    const sentDate = preferredStartDate.subMonths(4)
    const applicationId = randomId<ApplicationId>()

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
      `${testChild.lastName} ${testChild.firstName}`
    )
  })

  test('Placed child shows on report', async () => {
    const fixture = applicationFixture(
      testChild,
      testAdult,
      undefined,
      'PRESCHOOL',
      'AGREED'
    )

    const preferredStartDate = LocalDate.of(2021, 8, 13)
    const sentDate = preferredStartDate.subMonths(4)
    const applicationId = randomId<ApplicationId>()

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
      randomId<PlacementId>(),
      createdApplication.childId,
      preferredUnit.id,
      placementStartDate
    )
    await createDaycarePlacements({ body: [daycarePlacementFixture] })

    const report = await openPlacementSketchingReport()
    await report.assertRow(
      createdApplication.id,
      preferredUnit.name,
      `${testChild.lastName} ${testChild.firstName}`,
      currentUnit.name
    )
  })

  test('Application status filter works', async () => {
    const preferredStartDate = LocalDate.of(2021, 8, 13)
    const sentDate = preferredStartDate.subMonths(4)

    const fixtureForStatusSent = applicationFixture(
      testChild,
      testAdult,
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
      id: randomId<ApplicationId>()
    }

    const fixtureForStatusWaitingPlacement = applicationFixture(
      testChild2,
      testAdult,
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
      id: randomId<ApplicationId>()
    }

    const fixtureForStatusActive = applicationFixture(
      testChildRestricted,
      testAdult,
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
      confidential: true,
      id: randomId<ApplicationId>()
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
      `${testChild.lastName} ${testChild.firstName}`
    )
    await report.assertRow(
      applicationWithStatusWaitingPlacement.id,
      testDaycare.name,
      `${testChild2.lastName} ${testChild2.firstName}`
    )
    await report.assertRow(
      applicationWithStatusActive.id,
      testDaycare.name,
      `${testChildRestricted.lastName} ${testChildRestricted.firstName}`
    )

    await report.toggleApplicationStatus('SENT')
    await report.assertRow(
      applicationWithStatusSent.id,
      testDaycare.name,
      `${testChild.lastName} ${testChild.firstName}`
    )
    await report.assertNotRow(applicationWithStatusWaitingPlacement.id)
    await report.assertNotRow(applicationWithStatusActive.id)

    await report.toggleApplicationStatus('WAITING_PLACEMENT')
    await report.assertRow(
      applicationWithStatusSent.id,
      testDaycare.name,
      `${testChild.lastName} ${testChild.firstName}`
    )
    await report.assertRow(
      applicationWithStatusWaitingPlacement.id,
      testDaycare.name,
      `${testChild2.lastName} ${testChild2.firstName}`
    )
    await report.assertNotRow(applicationWithStatusActive.id)

    await report.toggleApplicationStatus('SENT')
    await report.assertNotRow(applicationWithStatusSent.id)
    await report.assertRow(
      applicationWithStatusWaitingPlacement.id,
      testDaycare.name,
      `${testChild2.lastName} ${testChild2.firstName}`
    )
    await report.assertNotRow(applicationWithStatusActive.id)
  })
})
