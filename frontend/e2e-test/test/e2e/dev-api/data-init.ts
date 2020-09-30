// SPDX-FileCopyrightText: 2017-2020 City of Espoo
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
  Fixture
} from './fixtures'
import * as devApi from '.'

const areaAndPersonFixtures = {
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
  personFixtureChildZeroYearOld
}

export type AreaAndPersonFixtures = typeof areaAndPersonFixtures

export const initializeAreaAndPersonData = async (): Promise<
  [typeof areaAndPersonFixtures, () => Promise<void>]
> => {
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
    .with(areaAndPersonFixtures.enduserChildJariOtherGuardianFixture)
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
    .with(areaAndPersonFixtures.restrictedPersonFixture)
    .save()

  await Fixture.person()
    .with(areaAndPersonFixtures.personFixtureChildZeroYearOld)
    .save()

  await devApi.insertChildFixtures([
    areaAndPersonFixtures.enduserChildFixtureJari,
    areaAndPersonFixtures.enduserChildFixtureKaarina,
    ...areaAndPersonFixtures.familyWithTwoGuardians.children,
    ...areaAndPersonFixtures.familyWithSeparatedGuardians.children,
    personFixtureChildZeroYearOld
  ])

  const cleanUp = async () => {
    await Fixture.cleanup()
  }

  return [areaAndPersonFixtures, cleanUp]
}
