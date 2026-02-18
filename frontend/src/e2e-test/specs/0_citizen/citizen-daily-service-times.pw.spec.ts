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
import type { DevDaycare, DevPerson } from '../../generated/api-types'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import { test } from '../../playwright'
import { waitUntilEqual } from '../../utils'
import { enduserLogin } from '../../utils/user'

const child = testChild
let daycare: DevDaycare
let guardian: DevPerson

test.beforeEach(async () => {
  await resetServiceState()

  const area = await testCareArea.save()
  daycare = await Fixture.daycare({ ...testDaycare, areaId: area.id }).save()
  await Fixture.daycareGroup({
    ...testDaycareGroup,
    daycareId: daycare.id
  }).save()

  const child1 = await child.saveChild({ updateMockVtj: true })
  guardian = await testAdult.saveAdult({
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

test.describe('Daily service times', () => {
  test('modal notification is shown when notification exists', async ({
    evaka
  }) => {
    await Fixture.dailyServiceTimeNotification({
      guardianId: guardian.id
    }).save()

    await enduserLogin(evaka, testAdult)
    const calendar = new CitizenCalendarPage(evaka, 'desktop')

    await waitUntilEqual(
      () => calendar.getDailyServiceTimeNotificationModalContent(),
      'Varhaiskasvatusaikasopimusta on muutettu, tarkistathan että varaukset vastaavat uutta sopimusaikaa.'
    )
  })
})
