// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import {
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testDaycare
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import { test, expect } from '../../playwright'
import type { Element } from '../../utils/page'
import { emulateRunningInstalled } from '../../utils/pwa'
import { enduserLogin } from '../../utils/user'

const today = LocalDate.of(2022, 1, 5)
const iphoneViewport = { width: 390, height: 844 }

test.use({
  viewport: iphoneViewport,
  hasTouch: true,
  evakaOptions: { mockedTime: today.toHelsinkiDateTime(LocalTime.of(12, 0)) }
})

// The installed app lays the page out as an app shell where only the content
// area scrolls (see citizen-frontend/App.tsx). A modal must still cover the
// whole screen there, so its footer buttons must not end up under the bottom
// navigation.
const expectReachable = async (button: Element) => {
  await expect(button).toBeInViewport({ ratio: 1 })
  await expect(button).toBeVisible()
  const topmostAtCenter = await button.locator.evaluate((el) => {
    const { left, top, width, height } = el.getBoundingClientRect()
    const hit = document.elementFromPoint(left + width / 2, top + height / 2)
    return hit === null ? 'nothing' : el.contains(hit) ? 'self' : hit.tagName
  })
  expect(topmostAtCenter, 'element covering the button').toBe('self')
}

test.describe('Citizen modals in the installed app', () => {
  let calendar: CitizenCalendarPage

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    await testCareArea.save()
    await testDaycare.save()
    await Fixture.family({ guardian: testAdult, children: [testChild] }).save()
    await Fixture.placement({
      childId: testChild.id,
      unitId: testDaycare.id,
      startDate: today,
      endDate: today.addYears(1)
    }).save()

    await emulateRunningInstalled(evaka)
    await enduserLogin(evaka, testAdult, '/calendar')
    await expect(evaka.find('html')).toHaveAttribute('data-standalone', '')
    calendar = new CitizenCalendarPage(evaka, 'mobile')
    await calendar.waitUntilLoaded()
  })

  test('reservation modal buttons are reachable', async ({ evaka }) => {
    await calendar.openReservationModal()
    await expectReachable(evaka.findByDataQa('modal-cancelBtn'))
    await expectReachable(evaka.findByDataQa('modal-okBtn'))
  })

  test('absence modal buttons are reachable', async ({ evaka }) => {
    await calendar.openAbsencesModal()
    await expectReachable(evaka.findByDataQa('modal-cancelBtn'))
    await expectReachable(evaka.findByDataQa('modal-okBtn'))
  })

  test('day view buttons are reachable', async () => {
    const dayView = await calendar.openDayView(today.addDays(7))
    await expectReachable(dayView.findByDataQa('create-absence'))
  })
})
