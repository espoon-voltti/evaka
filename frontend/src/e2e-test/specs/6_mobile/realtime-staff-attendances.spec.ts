// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { insertDefaultServiceNeedOptions, resetDatabase } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import { daycareGroupFixture, Fixture } from '../../dev-api/fixtures'
import MobileNav from '../../pages/mobile/mobile-nav'
import { StaffAttendancePage } from '../../pages/mobile/staff-page'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let nav: MobileNav
let mobileSignupUrl: string
let staffAttendancePage: StaffAttendancePage

beforeEach(async () => {
  await resetDatabase()
  const fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()

  await Fixture.daycareGroup().with(daycareGroupFixture).save()
  const daycarePlacementFixture = await Fixture.placement()
    .with({
      childId: fixtures.familyWithTwoGuardians.children[0].id,
      unitId: fixtures.daycareFixture.id
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      daycarePlacementId: daycarePlacementFixture.data.id,
      daycareGroupId: daycareGroupFixture.id
    })
    .save()

  page = await Page.open({
    employeeMobileCustomizations: {
      featureFlags: {
        experimental: {
          realtimeStaffAttendance: true
        }
      }
    }
  })
  nav = new MobileNav(page)

  mobileSignupUrl = await pairMobileDevice(fixtures.daycareFixture.id)
  await page.goto(mobileSignupUrl)
  await nav.openPage('staff')
  staffAttendancePage = new StaffAttendancePage(page)
})

describe('Realtime staff attendance page', () => {
  test('New staff member can be added and marked as departed', async () => {
    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.clickAddNewExternalMemberButton()

    await staffAttendancePage.setArrivedInfo(
      '03:20',
      'Nomen Estomen',
      daycareGroupFixture.name
    )

    await staffAttendancePage.assertPresentStaffCount(1)

    await staffAttendancePage.clickPresentTab()
    await staffAttendancePage.clickStaff(0)
    await staffAttendancePage.assertEmployeeStatus('Läsnä')
    await staffAttendancePage.clickMarkDepartedButton()
    await staffAttendancePage.assertPresentStaffCount(0)
  })
})
