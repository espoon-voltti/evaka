// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  testCareArea,
  testClub,
  clubTerms,
  testDaycare,
  testDaycarePrivateVoucher,
  testAdult,
  testAdult2,
  testAdultRestricted,
  testChild,
  testChild2,
  testChildDeceased,
  testChildNoSsn,
  testChildRestricted,
  testChildZeroYearOld,
  Fixture,
  testPreschool,
  preschoolTerms
} from './fixtures'

const areaAndPersonFixtures = {
  testCareArea,
  testClub,
  testDaycare,
  testDaycarePrivateVoucher,
  testPreschool,
  testAdult,
  testAdult2,
  testAdultRestricted,
  testChild,
  testChild2,
  testChildDeceased,
  testChildNoSsn,
  testChildRestricted,
  testChildZeroYearOld
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
  await Fixture.person()
    .with(areaAndPersonFixtures.testChild)
    .saveChild({ updateMockVtj: true })
  await Fixture.person()
    .with(areaAndPersonFixtures.testChild2)
    .saveChild({ updateMockVtj: true })
  await Fixture.person()
    .with(areaAndPersonFixtures.testChildRestricted)
    .saveChild({ updateMockVtj: true })
  await Fixture.person()
    .with(areaAndPersonFixtures.testAdult)
    .saveAdult({
      updateMockVtjWithDependants: [
        areaAndPersonFixtures.testChild,
        areaAndPersonFixtures.testChild2,
        areaAndPersonFixtures.testChildRestricted
      ]
    })
  await Fixture.person()
    .with(areaAndPersonFixtures.testAdult2)
    .saveAdult({
      updateMockVtjWithDependants: [areaAndPersonFixtures.testChild]
    })
  await Fixture.person()
    .with(areaAndPersonFixtures.testChildDeceased)
    .saveChild({ updateMockVtj: true })
  await Fixture.person()
    .with(areaAndPersonFixtures.testChildNoSsn)
    .with({ ssn: null })
    .saveChild()

  await Fixture.person()
    .with(areaAndPersonFixtures.testAdultRestricted)
    .saveAdult({ updateMockVtjWithDependants: [] })

  await Fixture.person()
    .with(areaAndPersonFixtures.testChildZeroYearOld)
    .saveChild()

  return areaAndPersonFixtures
}
