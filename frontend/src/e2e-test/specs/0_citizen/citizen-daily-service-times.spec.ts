// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import {
  testCareArea,
  testDaycare,
  testDaycareGroup,
  testChild,
  testAdult,
  Fixture
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { DevDaycare, DevPerson } from '../../generated/api-types'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page

const child = testChild
let daycare: DevDaycare
let guardian: DevPerson

beforeEach(async () => {
  await resetServiceState()
  page = await Page.open()

  const area = await Fixture.careArea(testCareArea).save()
  daycare = await Fixture.daycare({ ...testDaycare, areaId: area.id }).save()
  await Fixture.daycareGroup({
    ...testDaycareGroup,
    daycareId: daycare.id
  }).save()

  const child1 = await Fixture.person(child).saveChild({ updateMockVtj: true })
  guardian = await Fixture.person(testAdult).saveAdult({
    updateMockVtjWithDependants: [child1]
  })
  await Fixture.guardian(child1, guardian).save()
  await Fixture.placement({
    childId: child1.id,
    unitId: daycare.id,
    startDate: LocalDate.of(2020, 1, 1),
    endDate: LocalDate.of(2036, 6, 30)
  }).save()
  await Fixture.dailyServiceTime({
    childId: child1.id
  }).save()
})

describe('Daily service times', () => {
  test('modal notification is shown when notification exists', async () => {
    await Fixture.dailyServiceTimeNotification({
      guardianId: guardian.id
    }).save()

    await enduserLogin(page, testAdult)
    await new CitizenHeader(page).selectTab('calendar')
    const calendar = new CitizenCalendarPage(page, 'desktop')

    await waitUntilEqual(
      () => calendar.getDailyServiceTimeNotificationModalContent(),
      'Varhaiskasvatusaikasopimusta on muutettu, tarkistathan että varaukset vastaavat uutta sopimusaikaa.'
    )
  })
})
