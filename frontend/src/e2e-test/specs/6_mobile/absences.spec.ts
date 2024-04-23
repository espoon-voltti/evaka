// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import MobileAbsencesPage from '../../pages/mobile/absences-page'
import MobileChildPage from '../../pages/mobile/child-page'
import MobileListPage from '../../pages/mobile/list-page'
import { waitUntilEqual } from '../../utils'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let childId: UUID

let page: Page
let listPage: MobileListPage
let childPage: MobileChildPage
let absencesPage: MobileAbsencesPage

beforeEach(async () => {
  await resetServiceState()
  const area = await Fixture.careArea().save()
  const unit = await Fixture.daycare()
    .with({
      areaId: area.data.id,

      // MOBILE must be on
      // RESERVATIONS must be off
      enabledPilotFeatures: ['MOBILE']
    })
    .save()
  const group = await Fixture.daycareGroup()
    .with({
      daycareId: unit.data.id
    })
    .save()

  const person = await Fixture.person().save()
  const child = await Fixture.child(person.data.id).save()
  childId = child.data.id

  const daycarePlacementFixture = await Fixture.placement()
    .with({
      childId,
      unitId: unit.data.id
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      daycarePlacementId: daycarePlacementFixture.data.id,
      daycareGroupId: group.data.id
    })
    .save()

  page = await Page.open()
  listPage = new MobileListPage(page)
  childPage = new MobileChildPage(page)
  absencesPage = new MobileAbsencesPage(page)

  const mobileSignupUrl = await pairMobileDevice(unit.data.id)
  await page.goto(mobileSignupUrl)
})

describe('Future absences', () => {
  test('User can set and delete future absence periods', async () => {
    await listPage.selectChild(childId)
    await childPage.markAbsentBeforehandLink.click()
    await waitUntilEqual(() => absencesPage.getAbsencesCount(), 0)

    await absencesPage.markNewAbsencePeriod(
      LocalDate.todayInSystemTz().addWeeks(1),
      LocalDate.todayInSystemTz().addWeeks(2),
      'SICKLEAVE'
    )
    await childPage.markAbsentBeforehandLink.click()
    await waitUntilEqual(() => absencesPage.getAbsencesCount(), 1)

    await absencesPage.markNewAbsencePeriod(
      LocalDate.todayInSystemTz().addWeeks(4),
      LocalDate.todayInSystemTz().addWeeks(5),
      'SICKLEAVE'
    )
    await childPage.markAbsentBeforehandLink.click()
    await waitUntilEqual(() => absencesPage.getAbsencesCount(), 2)

    await absencesPage.deleteFirstAbsencePeriod()
    await waitUntilEqual(() => absencesPage.getAbsencesCount(), 1)
  })
})
