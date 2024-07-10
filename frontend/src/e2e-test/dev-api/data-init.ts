// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  careAreaFixture,
  clubFixture,
  clubTermFixtures,
  daycareFixture,
  daycareFixturePrivateVoucher,
  enduserChildFixtureJari,
  enduserChildFixtureKaarina,
  enduserChildFixturePorriHatterRestricted,
  enduserChildJariOtherGuardianFixture,
  enduserDeceasedChildFixture,
  enduserGuardianFixture,
  enduserNonSsnChildFixture,
  familyWithDeadGuardian,
  familyWithRestrictedDetailsGuardian,
  familyWithSeparatedGuardians,
  familyWithTwoGuardians,
  Fixture,
  personFixtureChildZeroYearOld,
  preschoolFixture,
  preschoolTermFixtures,
  restrictedPersonFixture
} from './fixtures'

const areaAndPersonFixtures = {
  careAreaFixture,
  clubFixture,
  daycareFixture,
  daycareFixturePrivateVoucher,
  preschoolFixture,
  enduserGuardianFixture,
  enduserChildFixtureJari,
  enduserChildFixtureKaarina,
  enduserChildFixturePorriHatterRestricted,
  enduserChildJariOtherGuardianFixture,
  enduserDeceasedChildFixture,
  enduserNonSsnChildFixture,
  familyWithTwoGuardians,
  familyWithSeparatedGuardians,
  restrictedPersonFixture,
  personFixtureChildZeroYearOld,
  familyWithRestrictedDetailsGuardian,
  familyWithDeadGuardian
}

export type AreaAndPersonFixtures = typeof areaAndPersonFixtures

export const initializeAreaAndPersonData = async (): Promise<
  typeof areaAndPersonFixtures
> => {
  for (const preschoolTermFixture of preschoolTermFixtures) {
    await Fixture.preschoolTerm().with(preschoolTermFixture).save()
  }
  for (const clubTermFixture of clubTermFixtures) {
    await Fixture.clubTerm().with(clubTermFixture).save()
  }
  const careArea = await Fixture.careArea()
    .with(areaAndPersonFixtures.careAreaFixture)
    .save()
  await Fixture.daycare()
    .with(areaAndPersonFixtures.clubFixture)
    .careArea(careArea)
    .save()
  await Fixture.daycare()
    .with(areaAndPersonFixtures.daycareFixture)
    .careArea(careArea)
    .save()
  await Fixture.daycare()
    .with(areaAndPersonFixtures.daycareFixturePrivateVoucher)
    .careArea(careArea)
    .save()
  await Fixture.daycare()
    .with(areaAndPersonFixtures.preschoolFixture)
    .careArea(careArea)
    .save()
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserChildFixtureJari)
    .saveChild({ updateMockVtj: true })
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserChildFixtureKaarina)
    .saveChild({ updateMockVtj: true })
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserChildFixturePorriHatterRestricted)
    .saveChild({ updateMockVtj: true })
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserGuardianFixture)
    .saveAdult({
      updateMockVtjWithDependants: [
        areaAndPersonFixtures.enduserChildFixtureJari,
        areaAndPersonFixtures.enduserChildFixtureKaarina,
        areaAndPersonFixtures.enduserChildFixturePorriHatterRestricted
      ]
    })
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserChildJariOtherGuardianFixture)
    .saveAdult({
      updateMockVtjWithDependants: [
        areaAndPersonFixtures.enduserChildFixtureJari
      ]
    })
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserDeceasedChildFixture)
    .saveChild({ updateMockVtj: true })
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserNonSsnChildFixture)
    .with({ ssn: null })
    .saveChild()

  await Promise.all(
    areaAndPersonFixtures.familyWithTwoGuardians.children.map(async (child) => {
      await Fixture.person().with(child).saveChild({ updateMockVtj: true })
    })
  )

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithTwoGuardians.guardian)
    .saveAdult({
      updateMockVtjWithDependants:
        areaAndPersonFixtures.familyWithTwoGuardians.children
    })

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithTwoGuardians.otherGuardian)
    .saveAdult({
      updateMockVtjWithDependants:
        areaAndPersonFixtures.familyWithTwoGuardians.children
    })

  await Promise.all(
    areaAndPersonFixtures.familyWithSeparatedGuardians.children.map(
      async (child) => {
        await Fixture.person().with(child).saveChild({ updateMockVtj: true })
      }
    )
  )

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithSeparatedGuardians.guardian)
    .saveAdult({
      updateMockVtjWithDependants:
        areaAndPersonFixtures.familyWithSeparatedGuardians.children
    })

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithSeparatedGuardians.otherGuardian)
    .saveAdult({
      updateMockVtjWithDependants:
        areaAndPersonFixtures.familyWithSeparatedGuardians.children
    })

  await Promise.all(
    areaAndPersonFixtures.familyWithRestrictedDetailsGuardian.children.map(
      async (child) => {
        await Fixture.person().with(child).saveChild({ updateMockVtj: true })
      }
    )
  )

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithRestrictedDetailsGuardian.guardian)
    .saveAdult({
      updateMockVtjWithDependants:
        areaAndPersonFixtures.familyWithRestrictedDetailsGuardian.children
    })

  await Fixture.person()
    .with(
      areaAndPersonFixtures.familyWithRestrictedDetailsGuardian.otherGuardian
    )
    .saveAdult({
      updateMockVtjWithDependants:
        areaAndPersonFixtures.familyWithRestrictedDetailsGuardian.children
    })

  await Fixture.person()
    .with(areaAndPersonFixtures.restrictedPersonFixture)
    .saveAdult({ updateMockVtjWithDependants: [] })

  await Fixture.person()
    .with(areaAndPersonFixtures.personFixtureChildZeroYearOld)
    .saveChild()

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithDeadGuardian.children[0])
    .saveChild({ updateMockVtj: true })
  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithDeadGuardian.guardian)
    .saveAdult({
      updateMockVtjWithDependants:
        areaAndPersonFixtures.familyWithDeadGuardian.children
    })

  return areaAndPersonFixtures
}
