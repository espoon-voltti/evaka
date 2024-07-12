// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  testCareArea,
  testClub,
  clubTerms,
  testDaycare,
  testDaycarePrivateVoucher,
  Fixture,
  testPreschool,
  preschoolTerms
} from './fixtures'

const areaAndPersonFixtures = {
  testCareArea,
  testClub,
  testDaycare,
  testDaycarePrivateVoucher,
  testPreschool
}

export type AreaAndPersonFixtures = typeof areaAndPersonFixtures

export const initializeAreaAndPersonData = async (): Promise<
  typeof areaAndPersonFixtures
> => {
  for (const preschoolTermFixture of preschoolTerms) {
    await Fixture.preschoolTerm().with(preschoolTermFixture).save()
  }
  for (const clubTermFixture of clubTerms) {
    await Fixture.clubTerm().with(clubTermFixture).save()
  }
  const careArea = await Fixture.careArea()
    .with(areaAndPersonFixtures.testCareArea)
    .save()
  await Fixture.daycare()
    .with(areaAndPersonFixtures.testClub)
    .careArea(careArea)
    .save()
  await Fixture.daycare()
    .with(areaAndPersonFixtures.testDaycare)
    .careArea(careArea)
    .save()
  await Fixture.daycare()
    .with(areaAndPersonFixtures.testDaycarePrivateVoucher)
    .careArea(careArea)
    .save()
  await Fixture.daycare()
    .with(areaAndPersonFixtures.testPreschool)
    .careArea(careArea)
    .save()

  return areaAndPersonFixtures
}
