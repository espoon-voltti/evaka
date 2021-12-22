// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  daycareGroupFixture,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import MobileNav from 'e2e-playwright/pages/mobile/mobile-nav'
import StaffPage from 'e2e-playwright/pages/mobile/staff-page'
import {
  insertDefaultServiceNeedOptions,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { waitUntilEqual, waitUntilTrue } from 'e2e-playwright/utils'
import { DaycareGroup } from 'e2e-test-common/dev-api/types'
import { pairMobileDevice } from 'e2e-playwright/utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let nav: MobileNav
let staffPage: StaffPage

const daycareGroup2Fixture: DaycareGroup = {
  ...daycareGroupFixture,
  id: uuidv4(),
  name: 'Ryhmä 2'
}

beforeEach(async () => {
  await resetDatabase()
  const fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()

  await Fixture.daycareGroup().with(daycareGroupFixture).save()
  await Fixture.daycareGroup().with(daycareGroup2Fixture).save()
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

  page = await Page.open()
  nav = new MobileNav(page)
  staffPage = new StaffPage(page)

  const mobileSignupUrl = await pairMobileDevice(fixtures.daycareFixture.id)
  await page.goto(mobileSignupUrl)
  await nav.openPage('staff')
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
    await nav.selectGroup(daycareGroupFixture.id)
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
    await nav.selectGroup(daycareGroupFixture.id)
    await staffPage.incStaffCount()
    await staffPage.confirm()

    await staffPage.incStaffCount()
    await staffPage.incStaffOtherCount()
    await staffPage.cancel()

    await waitUntilEqual(() => staffPage.staffCount, '0,5')
    await waitUntilEqual(() => staffPage.staffOtherCount, '0')
  })

  test('Button state', async () => {
    await nav.selectGroup(daycareGroupFixture.id)
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
