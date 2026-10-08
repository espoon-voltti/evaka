// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'

import {
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testDaycare,
  testDaycareGroup
} from '../../e2e-test/dev-api/fixtures'
import {
  createDaycareGroups,
  createMessageAccounts,
  insertGuardians,
  resetServiceState
} from '../../e2e-test/generated/api-clients'

export { testAdult, testChild }

export async function createFamilyWithPlacement() {
  const today = LocalDate.todayInHelsinkiTz()
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()
  const placement = await Fixture.placement({
    childId: testChild.id,
    unitId: testDaycare.id,
    startDate: today,
    endDate: today.addYears(1)
  }).save()
  return { today, placement }
}

export async function createFamilyWithMessaging() {
  const { today, placement } = await createFamilyWithPlacement()
  const now = HelsinkiDateTime.now()
  await createDaycareGroups({ body: [testDaycareGroup] })
  await Fixture.employee()
    .staff(testDaycare.id)
    .groupAcl(testDaycareGroup.id, now, now)
    .save()
  await Fixture.groupPlacement({
    daycarePlacementId: placement.id,
    daycareGroupId: testDaycareGroup.id,
    startDate: today,
    endDate: today.addYears(1)
  }).save()
  await insertGuardians({
    body: [{ childId: testChild.id, guardianId: testAdult.id }]
  })
  await createMessageAccounts()
}
