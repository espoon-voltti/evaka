// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  careAreaFixture,
  clubFixture,
  daycareFixture,
  preschoolFixture,
  enduserGuardianFixture,
  enduserChildFixtureJari,
  enduserChildFixtureKaarina,
  enduserChildJariOtherGuardianFixture,
  familyWithTwoGuardians,
  familyWithSeparatedGuardians,
  restrictedPersonFixture,
  personFixtureChildZeroYearOld,
  familyWithRestrictedDetailsGuardian,
  Fixture,
  enduserChildFixturePorriHatterRestricted,
  familyWithDeadGuardian,
  enduserDeceasedChildFixture,
  enduserNonSsnChildFixture,
  daycareFixturePrivateVoucher,
  preschoolTermFixtures,
  clubTermFixtures
} from './fixtures'

import * as devApi from '.'

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
    .with(areaAndPersonFixtures.enduserGuardianFixture)
    .save()
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserChildFixtureJari)
    .save()
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserChildFixtureKaarina)
    .save()
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserChildFixturePorriHatterRestricted)
    .save()
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserChildJariOtherGuardianFixture)
    .save()
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserDeceasedChildFixture)
    .save()
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserNonSsnChildFixture)
    .with({ ssn: undefined })
    .save()
  await devApi.insertChildFixtures([])

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithTwoGuardians.guardian)
    .saveAndUpdateMockVtj()

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithTwoGuardians.otherGuardian)
    .saveAndUpdateMockVtj()

  await Promise.all(
    areaAndPersonFixtures.familyWithTwoGuardians.children.map(async (child) => {
      await Fixture.person().with(child).saveAndUpdateMockVtj()
    })
  )

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithSeparatedGuardians.guardian)
    .saveAndUpdateMockVtj()

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithSeparatedGuardians.otherGuardian)
    .saveAndUpdateMockVtj()

  await Promise.all(
    areaAndPersonFixtures.familyWithSeparatedGuardians.children.map(
      async (child) => {
        await Fixture.person().with(child).saveAndUpdateMockVtj()
      }
    )
  )

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithRestrictedDetailsGuardian.guardian)
    .saveAndUpdateMockVtj()

  await Fixture.person()
    .with(
      areaAndPersonFixtures.familyWithRestrictedDetailsGuardian.otherGuardian
    )
    .saveAndUpdateMockVtj()

  await Promise.all(
    areaAndPersonFixtures.familyWithRestrictedDetailsGuardian.children.map(
      async (child) => {
        await Fixture.person().with(child).saveAndUpdateMockVtj()
      }
    )
  )

  await Fixture.person()
    .with(areaAndPersonFixtures.restrictedPersonFixture)
    .save()

  await Fixture.person()
    .with(areaAndPersonFixtures.personFixtureChildZeroYearOld)
    .save()

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithDeadGuardian.guardian)
    .saveAndUpdateMockVtj()
  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithDeadGuardian.children[0])
    .saveAndUpdateMockVtj()

  await devApi.insertChildFixtures([
    areaAndPersonFixtures.enduserChildFixtureJari,
    areaAndPersonFixtures.enduserChildFixtureKaarina,
    areaAndPersonFixtures.enduserChildFixturePorriHatterRestricted,
    ...areaAndPersonFixtures.familyWithTwoGuardians.children,
    ...areaAndPersonFixtures.familyWithSeparatedGuardians.children,
    ...areaAndPersonFixtures.familyWithRestrictedDetailsGuardian.children,
    ...areaAndPersonFixtures.familyWithDeadGuardian.children,
    personFixtureChildZeroYearOld
  ])

  return areaAndPersonFixtures
}
