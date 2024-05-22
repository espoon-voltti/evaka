// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createChildren } from '../generated/api-clients'

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
    .saveAndUpdateMockVtj()
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserChildFixtureKaarina)
    .saveAndUpdateMockVtj()
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserChildFixturePorriHatterRestricted)
    .saveAndUpdateMockVtj()
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserGuardianFixture)
    .withDependants(
      areaAndPersonFixtures.enduserChildFixtureJari,
      areaAndPersonFixtures.enduserChildFixtureKaarina,
      areaAndPersonFixtures.enduserChildFixturePorriHatterRestricted
    )
    .saveAndUpdateMockVtj()
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserChildJariOtherGuardianFixture)
    .withDependants(areaAndPersonFixtures.enduserChildFixtureJari)
    .saveAndUpdateMockVtj()
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserDeceasedChildFixture)
    .saveAndUpdateMockVtj()
  await Fixture.person()
    .with(areaAndPersonFixtures.enduserNonSsnChildFixture)
    .with({ ssn: undefined })
    .save()

  await Promise.all(
    areaAndPersonFixtures.familyWithTwoGuardians.children.map(async (child) => {
      await Fixture.person().with(child).saveAndUpdateMockVtj()
    })
  )

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithTwoGuardians.guardian)
    .saveAndUpdateMockVtj()

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithTwoGuardians.otherGuardian)
    .saveAndUpdateMockVtj()

  await Promise.all(
    areaAndPersonFixtures.familyWithSeparatedGuardians.children.map(
      async (child) => {
        await Fixture.person().with(child).saveAndUpdateMockVtj()
      }
    )
  )

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithSeparatedGuardians.guardian)
    .saveAndUpdateMockVtj()

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithSeparatedGuardians.otherGuardian)
    .saveAndUpdateMockVtj()

  await Promise.all(
    areaAndPersonFixtures.familyWithRestrictedDetailsGuardian.children.map(
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

  await Fixture.person()
    .with(areaAndPersonFixtures.restrictedPersonFixture)
    .saveAndUpdateMockVtj()

  await Fixture.person()
    .with(areaAndPersonFixtures.personFixtureChildZeroYearOld)
    .save()

  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithDeadGuardian.children[0])
    .saveAndUpdateMockVtj()
  await Fixture.person()
    .with(areaAndPersonFixtures.familyWithDeadGuardian.guardian)
    .saveAndUpdateMockVtj()

  await createChildren({
    body: [
      areaAndPersonFixtures.enduserChildFixtureJari,
      areaAndPersonFixtures.enduserChildFixtureKaarina,
      areaAndPersonFixtures.enduserChildFixturePorriHatterRestricted,
      ...areaAndPersonFixtures.familyWithTwoGuardians.children,
      ...areaAndPersonFixtures.familyWithSeparatedGuardians.children,
      ...areaAndPersonFixtures.familyWithRestrictedDetailsGuardian.children,
      ...areaAndPersonFixtures.familyWithDeadGuardian.children,
      personFixtureChildZeroYearOld
    ].map(({ id }) => ({
      id,
      additionalInfo: '',
      allergies: '',
      diet: '',
      dietId: null,
      mealTextureId: null,
      languageAtHome: '',
      languageAtHomeDetails: '',
      medication: ''
    }))
  })

  return areaAndPersonFixtures
}
