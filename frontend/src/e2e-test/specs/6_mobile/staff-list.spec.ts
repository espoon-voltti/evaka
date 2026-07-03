// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { mobileViewport } from '../../browser'
import { testDaycare2, testDaycareGroup, Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type {
  DevCareArea,
  DevDaycareGroup,
  DevEmployee
} from '../../generated/api-types'
import MobileNav from '../../pages/mobile/mobile-nav'
import { StaffAttendancePage } from '../../pages/mobile/staff-page'
import { test } from '../../playwright'
import { pairMobileDevice } from '../../utils/mobile'
import type { Page } from '../../utils/page'

const daycareGroup2Fixture: DevDaycareGroup = {
  ...testDaycareGroup,
  id: randomId(),
  name: 'Ryhmä 2'
}

const pin = '2468'

test.describe('Staff attendance list', () => {
  let page: Page
  let nav: MobileNav
  let staffPage: StaffAttendancePage

  let careArea: DevCareArea
  let ankka: DevEmployee

  test.use({
    viewport: mobileViewport,
    evakaOptions: {
      mockedTime: HelsinkiDateTime.fromLocal(
        LocalDate.of(2026, 7, 2),
        LocalTime.of(6, 0)
      )
    }
  })

  const initPages = async () => {
    nav = new MobileNav(page)
    staffPage = new StaffAttendancePage(page)

    const mobileSignupUrl = await pairMobileDevice(testDaycare2.id)
    await page.goto(mobileSignupUrl)
    await nav.staff.click()
  }

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()

    careArea = await Fixture.careArea().save()
    await Fixture.daycare({
      ...testDaycare2,
      areaId: careArea.id,
      enabledPilotFeatures: ['REALTIME_STAFF_ATTENDANCE']
    }).save()

    await Fixture.daycareGroup({
      ...testDaycareGroup,
      daycareId: testDaycare2.id
    }).save()
    await Fixture.daycareGroup({
      ...daycareGroup2Fixture,
      daycareId: testDaycare2.id
    }).save()

    ankka = await Fixture.employee({
      firstName: 'Antero',
      lastName: 'Ankka'
    })
      .staff(testDaycare2.id)
      .groupAcl(testDaycareGroup.id)
      .save()
    await Fixture.employeePin({ userId: ankka.id, pin }).save()
    await Fixture.employee({
      firstName: 'Mikki',
      lastName: 'Hiiri'
    })
      .staff(testDaycare2.id)
      .groupAcl(daycareGroup2Fixture.id)
      .save()

    page = evaka
  })

  test('shows present and absent staff in a single view', async () => {
    await initPages()

    await staffPage.assertPresentStaffCount(0)
    await staffPage.assertAbsentStaffCount(2)
    await staffPage.assertStaffInSection('absent', 'Ankka')
    await staffPage.assertStaffInSection('absent', 'Hiiri')
  })

  test('sections can be collapsed and expanded', async () => {
    await initPages()

    await staffPage.assertStaffInSection('absent', 'Ankka')
    await staffPage.toggleSection('absent')
    await staffPage.assertStaffInSection('absent', 'Ankka', false)
    await staffPage.toggleSection('absent')
    await staffPage.assertStaffInSection('absent', 'Ankka')
  })

  test('staff can be searched by name', async () => {
    await initPages()

    await staffPage.openSearch()
    await staffPage.searchStaff('Antero')
    await staffPage.assertSearchResultVisible('Ankka')
    await staffPage.assertSearchResultVisible('Hiiri', false)
  })

  test('search covers the whole unit regardless of the selected group', async () => {
    await initPages()

    // Narrow the list to group 2, which only Hiiri belongs to
    await nav.selectGroup(daycareGroup2Fixture.id)
    await staffPage.assertStaffInSection('absent', 'Hiiri')
    await staffPage.assertStaffInSection('absent', 'Ankka', false)

    // Search still finds Ankka, who belongs to group 1
    await staffPage.openSearch()
    await staffPage.searchStaff('Antero')
    await staffPage.assertSearchResultVisible('Ankka')
  })

  test('marking a staff member arrived moves them from absent to present', async () => {
    await initPages()

    await staffPage.assertPresentStaffCount(0)
    await staffPage.assertAbsentStaffCount(2)
    await staffPage.assertStaffInSection('absent', 'Ankka')

    await staffPage.openStaffPage('Ankka')
    await staffPage.markStaffArrived({
      pin,
      time: '05:59',
      group: testDaycareGroup
    })
    await staffPage.assertEmployeeStatus('Läsnä')
    await staffPage.goBackFromMemberPage()

    await staffPage.assertPresentStaffCount(1)
    await staffPage.assertAbsentStaffCount(1)
    await staffPage.assertStaffInSection('present', 'Ankka')
    await staffPage.assertStaffInSection('absent', 'Ankka', false)
  })
})
