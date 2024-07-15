// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import config from '../../config'
import {
  applicationFixture,
  applicationFixtureId,
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testChild2,
  testDaycare,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createApplications,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let admin: DevEmployee

beforeEach(async () => {
  await resetServiceState()
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  admin = await Fixture.employee().admin().save()
})

async function openPage(employee: DevEmployee = admin) {
  const page = await Page.open()
  await employeeLogin(page, employee)
  await page.goto(config.adminUrl)
  return new ApplicationListView(page)
}

describe('Employee searches applications', () => {
  test('Duplicate applications are found', async () => {
    const fixture = applicationFixture(testChild, testAdult)

    const duplicateFixture = {
      ...applicationFixture(testChild, testAdult),
      id: '9dd0e1ba-9b3b-11ea-bb37-0242ac130222'
    }

    const nonDuplicateFixture = {
      ...applicationFixture(testChild2, testAdult),
      id: '9dd0e1ba-9b3b-11ea-bb37-0242ac130224'
    }

    await createApplications({
      body: [fixture, duplicateFixture, nonDuplicateFixture]
    })
    const applicationListView = await openPage()

    await applicationListView.specialFilterItems.duplicate.click()
    await applicationListView.assertApplicationIsVisible(applicationFixtureId)
    await applicationListView.assertApplicationIsVisible(duplicateFixture.id)
    await applicationListView.assertApplicationCount(2)
  })

  test('Care area filters work', async () => {
    const careArea1 = await Fixture.careArea().save()
    const daycare1 = await Fixture.daycare({ areaId: careArea1.id }).save()
    const careArea2 = await Fixture.careArea().save()
    const daycare2 = await Fixture.daycare({ areaId: careArea2.id }).save()
    const careArea3 = await Fixture.careArea().save()
    const daycare3 = await Fixture.daycare({ areaId: careArea3.id }).save()

    const createApplicationForUnit = (unitId: string) => ({
      ...applicationFixture(testChild, testAdult, undefined, 'DAYCARE', null, [
        unitId
      ]),
      id: uuidv4()
    })

    const app1 = createApplicationForUnit(daycare1.id)
    const app2 = createApplicationForUnit(daycare2.id)
    const app3 = createApplicationForUnit(daycare3.id)

    await createApplications({ body: [app1, app2, app3] })
    const applicationListView = await openPage()

    await applicationListView.assertApplicationCount(3)

    await applicationListView.toggleArea(careArea1.name)
    await applicationListView.assertApplicationCount(1)
    await applicationListView.assertApplicationIsVisible(app1.id)

    await applicationListView.toggleArea(careArea2.name)
    await applicationListView.assertApplicationCount(2)
    await applicationListView.assertApplicationIsVisible(app1.id)
    await applicationListView.assertApplicationIsVisible(app2.id)
  })

  test('Unit filter works', async () => {
    const careArea1 = await Fixture.careArea().save()
    const daycare1 = await Fixture.daycare({ areaId: careArea1.id }).save()
    const daycare2 = await Fixture.daycare({ areaId: careArea1.id }).save()

    const createApplicationForUnit = (unitId: string) => ({
      ...applicationFixture(testChild, testAdult, undefined, 'DAYCARE', null, [
        unitId
      ]),
      id: uuidv4()
    })

    const app1 = createApplicationForUnit(daycare1.id)
    const app2 = createApplicationForUnit(daycare2.id)

    await createApplications({ body: [app1, app2] })
    const applicationListView = await openPage()

    await applicationListView.assertApplicationCount(2)

    await applicationListView.toggleUnit(daycare1.name)
    await applicationListView.assertApplicationIsVisible(app1.id)
    await applicationListView.assertApplicationCount(1)
  })

  test('Special education teacher (VEO) sees only allowed applications', async () => {
    const careArea1 = await Fixture.careArea().save()
    const daycare1 = await Fixture.daycare({ areaId: careArea1.id }).save()
    const daycare2 = await Fixture.daycare({ areaId: careArea1.id }).save()

    const specialEducationTeacher = await Fixture.employee()
      .specialEducationTeacher(daycare1.id)
      .save()

    const createApplicationForUnit = (
      unitId: string,
      assistanceNeeded: boolean
    ) => ({
      ...applicationFixture(
        testChild,
        testAdult,
        undefined,
        'DAYCARE',
        null,
        [unitId],
        false,
        'SENT',
        LocalDate.of(2021, 8, 16),
        false,
        assistanceNeeded
      ),
      id: uuidv4()
    })

    const appWithAssistanceNeeded = createApplicationForUnit(daycare1.id, true)
    const appWithoutAssistanceNeeded = createApplicationForUnit(
      daycare1.id,
      false
    )
    const appWithAssistanceNeededWrongUnit = createApplicationForUnit(
      daycare2.id,
      true
    )

    await createApplications({
      body: [
        appWithAssistanceNeeded,
        appWithoutAssistanceNeeded,
        appWithAssistanceNeededWrongUnit
      ]
    })
    const applicationListView = await openPage(specialEducationTeacher)
    await applicationListView.assertApplicationCount(1)
  })

  test('Voucher application filter works', async () => {
    const careArea1 = await Fixture.careArea().save()
    const voucherUnit = await Fixture.daycare({
      areaId: careArea1.id,
      providerType: 'PRIVATE_SERVICE_VOUCHER'
    }).save()
    const municipalUnit = await Fixture.daycare({
      areaId: careArea1.id,
      providerType: 'MUNICIPAL'
    }).save()

    const applicationWithVoucherUnitFirst = {
      ...applicationFixture(testChild, testAdult, undefined, 'DAYCARE', null, [
        voucherUnit.id
      ]),
      id: uuidv4()
    }
    const applicationWithVoucherUnitSecond = {
      ...applicationFixture(testChild, testAdult, undefined, 'DAYCARE', null, [
        municipalUnit.id,
        voucherUnit.id
      ]),
      id: uuidv4()
    }
    const applicationWithNoVoucherUnit = {
      ...applicationFixture(testChild, testAdult, undefined, 'DAYCARE', null, [
        municipalUnit.id
      ]),
      id: uuidv4()
    }

    await createApplications({
      body: [
        applicationWithVoucherUnitFirst,
        applicationWithVoucherUnitSecond,
        applicationWithNoVoucherUnit
      ]
    })
    const applicationListView = await openPage()

    await applicationListView.voucherUnitFilter.noFilter.click()
    await applicationListView.assertApplicationCount(3)

    await applicationListView.voucherUnitFilter.firstChoice.click()
    await applicationListView.assertApplicationIsVisible(
      applicationWithVoucherUnitFirst.id
    )
    await applicationListView.assertApplicationCount(1)

    await applicationListView.voucherUnitFilter.voucherOnly.click()
    await applicationListView.assertApplicationIsVisible(
      applicationWithVoucherUnitFirst.id
    )
    await applicationListView.assertApplicationIsVisible(
      applicationWithVoucherUnitSecond.id
    )
    await applicationListView.assertApplicationCount(2)

    await applicationListView.voucherUnitFilter.voucherHide.click()
    await applicationListView.assertApplicationIsVisible(
      applicationWithNoVoucherUnit.id
    )
    await applicationListView.assertApplicationCount(1)
  })
  test('Application type filters work', async () => {
    const careArea = await Fixture.careArea().save()
    const club = await Fixture.daycare({
      areaId: careArea.id,
      type: ['CLUB']
    }).save()
    const daycare = await Fixture.daycare({
      areaId: careArea.id,
      type: ['CENTRE']
    }).save()
    const preschool = await Fixture.daycare({
      areaId: careArea.id,
      type: ['PRESCHOOL']
    }).save()
    const clubApplication = {
      ...applicationFixture(testChild, testAdult, undefined, 'CLUB', null, [
        club.id
      ]),
      id: uuidv4()
    }
    const daycareApplication = {
      ...applicationFixture(testChild, testAdult, undefined, 'DAYCARE', null, [
        daycare.id
      ]),
      id: uuidv4()
    }
    const preschoolApplication = {
      ...applicationFixture(
        testChild,
        testAdult,
        undefined,
        'PRESCHOOL',
        null,
        [preschool.id]
      ),
      id: uuidv4()
    }
    await createApplications({
      body: [clubApplication, daycareApplication, preschoolApplication]
    })
    const applicationListView = await openPage()

    await applicationListView.filterByApplicationType('ALL')
    await applicationListView.assertApplicationCount(3)

    await applicationListView.filterByApplicationType('CLUB')
    await applicationListView.assertApplicationCount(1)
    await applicationListView.assertApplicationIsVisible(clubApplication.id)

    await applicationListView.filterByApplicationType('DAYCARE')
    await applicationListView.assertApplicationCount(1)
    await applicationListView.assertApplicationIsVisible(daycareApplication.id)

    await applicationListView.filterByApplicationType('PRESCHOOL')
    await applicationListView.assertApplicationCount(1)
    await applicationListView.assertApplicationIsVisible(
      preschoolApplication.id
    )
  })
})
