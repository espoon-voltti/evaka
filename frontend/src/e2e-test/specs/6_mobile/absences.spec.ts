// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { PersonId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import MobileAbsencesPage from '../../pages/mobile/absences-page'
import MobileChildPage from '../../pages/mobile/child-page'
import MobileListPage from '../../pages/mobile/list-page'
import { test, expect } from '../../playwright'
import { pairMobileDevice } from '../../utils/mobile'
import type { Page } from '../../utils/page'

const mockedNow = HelsinkiDateTime.of(2024, 11, 20, 13, 0)
const today = mockedNow.toLocalDate()

test.describe('Future absences', () => {
  test.use({ evakaOptions: { mockedTime: mockedNow } })

  let childId: PersonId
  let page: Page
  let listPage: MobileListPage
  let childPage: MobileChildPage
  let absencesPage: MobileAbsencesPage

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({
      areaId: area.id,

      // MOBILE must be on
      // RESERVATIONS must be off
      enabledPilotFeatures: ['MOBILE']
    }).save()
    const group = await Fixture.daycareGroup({
      daycareId: unit.id
    }).save()

    const person = await Fixture.person().saveChild()
    childId = person.id

    const daycarePlacementFixture = await Fixture.placement({
      childId,
      unitId: unit.id,
      startDate: today,
      endDate: today.addYears(1)
    }).save()

    await Fixture.groupPlacement({
      daycarePlacementId: daycarePlacementFixture.id,
      daycareGroupId: group.id,
      startDate: daycarePlacementFixture.startDate,
      endDate: daycarePlacementFixture.endDate
    }).save()

    page = evaka
    listPage = new MobileListPage(page)
    childPage = new MobileChildPage(page)
    absencesPage = new MobileAbsencesPage(page)

    const mobileSignupUrl = await pairMobileDevice(unit.id)
    await page.goto(mobileSignupUrl)
  })

  test('User can set and delete future absence periods', async () => {
    await listPage.selectChild(childId)
    await childPage.markAbsentBeforehandLink.click()
    await expect.poll(() => absencesPage.getAbsencesCount()).toBe(0)

    await absencesPage.markNewAbsencePeriod(
      today.addWeeks(1),
      today.addWeeks(2),
      'SICKLEAVE'
    )
    await childPage.markAbsentBeforehandLink.click()
    await expect.poll(() => absencesPage.getAbsencesCount()).toBe(1)

    await absencesPage.markNewAbsencePeriod(
      today.addWeeks(4),
      today.addWeeks(5),
      'SICKLEAVE'
    )
    await childPage.markAbsentBeforehandLink.click()
    await expect.poll(() => absencesPage.getAbsencesCount()).toBe(2)

    await absencesPage.deleteFirstAbsencePeriod()
    await expect.poll(() => absencesPage.getAbsencesCount()).toBe(1)
  })
})
