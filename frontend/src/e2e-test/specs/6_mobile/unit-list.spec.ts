// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import {
  testDaycareGroup,
  Fixture,
  testCareArea,
  testPreschool
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { DevDaycare, DevDaycareGroup } from '../../generated/api-types'
import UnitListPage from '../../pages/mobile/unit-list-page'
import { pairPersonalMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let unitListPage: UnitListPage
let unit: DevDaycare
let group: DevDaycareGroup

const mockedNow = HelsinkiDateTime.of(2022, 7, 31, 13, 0)

beforeEach(async () => {
  await resetServiceState()
  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare().with(testPreschool).save()

  unit = await Fixture.daycare()
    .with({
      areaId: testCareArea.id,
      enabledPilotFeatures: ['REALTIME_STAFF_ATTENDANCE']
    })
    .save()
  group = await Fixture.daycareGroup()
    .with({
      ...testDaycareGroup,
      daycareId: unit.id
    })
    .save()

  page = await Page.open({ mockedTime: mockedNow })

  const unitSupervisor = await Fixture.employeeUnitSupervisor(unit.id)
    .withDaycareAcl(testPreschool.id, 'UNIT_SUPERVISOR')
    .save()
  const mobileSignupUrl = await pairPersonalMobileDevice(unitSupervisor.id)
  await page.goto(mobileSignupUrl)
  unitListPage = new UnitListPage(page)
})

describe('Employee mobile unit list', () => {
  test('Staff count is as expected', async () => {
    const staff1Fixture = await Fixture.employeeStaff(unit.id)
      .withGroupAcl(testDaycareGroup.id)
      .save()
    const staff2Fixture = await Fixture.employeeStaff(unit.id)
      .withGroupAcl(testDaycareGroup.id)
      .save()

    await Fixture.realtimeStaffAttendance()
      .with({
        employeeId: staff1Fixture.id,
        groupId: group.id,
        arrived: mockedNow.subHours(5),
        occupancyCoefficient: 7
      })
      .save()
    // Not included in staff count because occupancyCoefficient is 0
    await Fixture.realtimeStaffAttendance()
      .with({
        employeeId: staff2Fixture.id,
        groupId: group.id,
        arrived: mockedNow.subHours(5),
        occupancyCoefficient: 0
      })
      .save()

    await page.reload()
    await unitListPage.assertStaffCount(unit.id, 1, 0)
  })
})
