// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  testDaycareGroup,
  Fixture,
  familyWithTwoGuardians
} from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import MobileNav from '../../pages/mobile/mobile-nav'
import StaffPage from '../../pages/mobile/staff-page'
import { waitUntilEqual, waitUntilTrue } from '../../utils'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let nav: MobileNav
let staffPage: StaffPage
let mobileSignupUrl: string

const now = HelsinkiDateTime.of(2023, 3, 15, 12, 0)
const today = now.toLocalDate()

beforeEach(async () => {
  await resetServiceState()
  const fixtures = await initializeAreaAndPersonData()
  await Fixture.family(familyWithTwoGuardians).save()
  await createDefaultServiceNeedOptions()

  await Fixture.daycareGroup().with(testDaycareGroup).save()
  const daycarePlacementFixture = await Fixture.placement()
    .with({
      childId: familyWithTwoGuardians.children[0].id,
      unitId: fixtures.testDaycare.id,
      startDate: today,
      endDate: today.addYears(1)
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      daycarePlacementId: daycarePlacementFixture.id,
      daycareGroupId: testDaycareGroup.id,
      startDate: today,
      endDate: today.addYears(1)
    })
    .save()

  page = await Page.open({ mockedTime: now })
  nav = new MobileNav(page)

  mobileSignupUrl = await pairMobileDevice(fixtures.testDaycare.id)
  await page.goto(mobileSignupUrl)
  await nav.staff.click()
  staffPage = new StaffPage(page)
})

describe('Staff page', () => {
  test('Staff for all groups is read-only', async () => {
    await waitUntilEqual(() => staffPage.staffCount, '0')
    await waitUntilEqual(() => staffPage.staffOtherCount, '0')
    await waitUntilEqual(() => staffPage.updated, 'Tietoja ei ole päivitetty')

    await waitUntilEqual(
      () => staffPage.incDecButtonsVisible(),
      [false, false, false, false]
    )
  })

  test('Set group staff', async () => {
    await nav.selectGroup(testDaycareGroup.id)
    await waitUntilEqual(() => staffPage.staffCount, '0')
    await waitUntilEqual(() => staffPage.staffOtherCount, '0')
    await waitUntilEqual(() => staffPage.updated, 'Tietoja ei ole päivitetty')

    await staffPage.incStaffCount()
    await staffPage.incStaffCount()
    await staffPage.incStaffCount()
    await waitUntilEqual(() => staffPage.staffCount, '1,5')
    await staffPage.incStaffOtherCount()
    await waitUntilEqual(() => staffPage.staffOtherCount, '0,5')
    await staffPage.confirm()

    await waitUntilEqual(
      () => staffPage.occupancy,
      'Ryhmän käyttöaste tänään 9,5 %'
    )
    await waitUntilTrue(async () =>
      (await staffPage.updated).startsWith('Tiedot päivitetty tänään ')
    )
  })

  test('Cancel resets the form', async () => {
    await nav.selectGroup(testDaycareGroup.id)
    await staffPage.incStaffCount()
    await staffPage.confirm()

    await staffPage.incStaffCount()
    await staffPage.incStaffOtherCount()
    await staffPage.cancel()

    await waitUntilEqual(() => staffPage.staffCount, '0,5')
    await waitUntilEqual(() => staffPage.staffOtherCount, '0')
  })

  test('Button state', async () => {
    await nav.selectGroup(testDaycareGroup.id)
    await waitUntilTrue(() => staffPage.buttonsDisabled)

    await staffPage.incStaffCount()
    await waitUntilTrue(() => staffPage.buttonsEnabled)

    await staffPage.decStaffCount()
    await waitUntilTrue(() => staffPage.buttonsDisabled)

    await staffPage.incStaffOtherCount()
    await waitUntilTrue(() => staffPage.buttonsEnabled)

    await staffPage.decStaffOtherCount()
    await waitUntilTrue(() => staffPage.buttonsDisabled)
  })
})
