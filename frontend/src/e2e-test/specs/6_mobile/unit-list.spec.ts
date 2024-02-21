// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  DaycareBuilder,
  DaycareGroupBuilder,
  daycareGroupFixture,
  Fixture
} from '../../dev-api/fixtures'
import { resetDatabase } from '../../generated/api-clients'
import UnitListPage from '../../pages/mobile/unit-list-page'
import { pairPersonalMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let unitListPage: UnitListPage
let unit: DaycareBuilder
let group: DaycareGroupBuilder

const mockedNow = HelsinkiDateTime.of(2022, 7, 31, 13, 0)

beforeEach(async () => {
  await resetDatabase()
  const fixtures = await initializeAreaAndPersonData()

  unit = await Fixture.daycare()
    .with({
      areaId: fixtures.careAreaFixture.id,
      enabledPilotFeatures: ['REALTIME_STAFF_ATTENDANCE']
    })
    .save()
  group = await Fixture.daycareGroup()
    .with({
      ...daycareGroupFixture,
      daycareId: unit.data.id
    })
    .save()

  page = await Page.open({ mockedTime: mockedNow })

  const unitSupervisor = await Fixture.employeeUnitSupervisor(unit.data.id)
    .withDaycareAcl(fixtures.preschoolFixture.id, 'UNIT_SUPERVISOR')
    .save()
  const mobileSignupUrl = await pairPersonalMobileDevice(unitSupervisor.data.id)
  await page.goto(mobileSignupUrl)
  unitListPage = new UnitListPage(page)
})

describe('Employee mobile unit list', () => {
  test('Staff count is as expected', async () => {
    const staff1Fixture = await Fixture.employeeStaff(unit.data.id)
      .withGroupAcl(daycareGroupFixture.id)
      .save()
    const staff2Fixture = await Fixture.employeeStaff(unit.data.id)
      .withGroupAcl(daycareGroupFixture.id)
      .save()

    await Fixture.realtimeStaffAttendance()
      .with({
        employeeId: staff1Fixture.data.id,
        groupId: group.data.id,
        arrived: mockedNow.subHours(5),
        occupancyCoefficient: 7
      })
      .save()
    // Not included in staff count because occupancyCoefficient is 0
    await Fixture.realtimeStaffAttendance()
      .with({
        employeeId: staff2Fixture.data.id,
        groupId: group.data.id,
        arrived: mockedNow.subHours(5),
        occupancyCoefficient: 0
      })
      .save()

    await page.reload()
    await unitListPage.assertStaffCount(unit.data.id, 1, 0)
  })
})
