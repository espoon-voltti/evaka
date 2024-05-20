// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import {
  careAreaFixture,
  daycare2Fixture,
  daycareFixture,
  daycareGroupFixture,
  enduserChildFixtureJari,
  enduserChildFixtureKaarina,
  enduserChildFixturePorriHatterRestricted,
  enduserNonSsnChildFixture,
  familyWithTwoGuardians,
  Fixture,
  preschoolTermFixture2021,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import ConfirmedDayReservationPage from '../../pages/mobile/child-confimed-reservations-page'
import MobileListPage from '../../pages/mobile/list-page'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let confirmedReservationPage: ConfirmedDayReservationPage
let attendanceListPage: MobileListPage
const now = HelsinkiDateTime.of(2022, 5, 17, 13, 0, 0)

const group2 = {
  id: uuidv4(),
  name: '#2',
  daycareId: daycareFixture.id,
  startDate: LocalDate.of(2021, 1, 1)
}

beforeEach(async () => {
  await resetServiceState()
  await createDefaultServiceNeedOptions()
  await insertConfirmedDaysTestData()

  const mobileSignupUrl = await pairMobileDevice(daycareFixture.id)
  page = await Page.open({ mockedTime: now })

  await page.goto(mobileSignupUrl)

  attendanceListPage = new MobileListPage(page)
  await attendanceListPage.confirmedDaysTab.click()

  confirmedReservationPage = new ConfirmedDayReservationPage(page)
})

describe('Child confirmed reservations', () => {
  test('Confirmed days are present', async () => {
    const confirmedDaysOnTestDate = [
      LocalDate.of(2022, 5, 18),
      LocalDate.of(2022, 5, 19),
      LocalDate.of(2022, 5, 20),
      LocalDate.of(2022, 5, 23),
      LocalDate.of(2022, 5, 25),
      LocalDate.of(2022, 5, 26),
      LocalDate.of(2022, 5, 27)
    ]

    for (const day of confirmedDaysOnTestDate) {
      await confirmedReservationPage.assertDayExists(day)
    }

    const nonConfirmedDaysOnTestDate = [
      LocalDate.of(2022, 5, 17),
      LocalDate.of(2022, 5, 24),
      LocalDate.of(2022, 5, 28)
    ]

    for (const day of nonConfirmedDaysOnTestDate) {
      await confirmedReservationPage.assertDayDoesNotExist(day)
    }
  })

  test('Daily counts are correct', async () => {
    const dayCounts = [
      {
        date: LocalDate.of(2022, 5, 18),
        presentCount: '3',
        presentCalc: '3,75',
        absentCount: '1'
      },
      {
        date: LocalDate.of(2022, 5, 19),
        presentCount: '2',
        presentCalc: '2,75',
        absentCount: '2'
      },
      {
        date: LocalDate.of(2022, 5, 20),
        presentCount: '3',
        presentCalc: '3,75',
        absentCount: '1'
      },
      {
        date: LocalDate.of(2022, 5, 23),
        presentCount: '4',
        presentCalc: '4,25',
        absentCount: '0'
      },
      {
        date: LocalDate.of(2022, 5, 25),
        presentCount: '4',
        presentCalc: '4,25',
        absentCount: '0'
      },
      {
        date: LocalDate.of(2022, 5, 26),
        presentCount: '3',
        presentCalc: '3,25',
        absentCount: '1'
      },
      {
        date: LocalDate.of(2022, 5, 27),
        presentCount: '4',
        presentCalc: '4,75',
        absentCount: '0'
      }
    ]
    for (const day of dayCounts) {
      await confirmedReservationPage.assertDailyCounts(
        day.date,
        day.presentCount,
        day.presentCalc,
        day.absentCount
      )
    }
  })
  test('Daily children are correct (Thursday)', async () => {
    const testDay = LocalDate.of(2022, 5, 19)
    await confirmedReservationPage.openDayItem(testDay)
    const expectedChildItems = [
      {
        date: testDay,
        childId: enduserNonSsnChildFixture.id,
        reservationTexts: ['Ei toimintaa'],
        childDetails: {
          firstName: enduserNonSsnChildFixture.firstName.split(/\s/)[0],
          lastName: enduserNonSsnChildFixture.lastName,
          preferredName: enduserNonSsnChildFixture.preferredName
        }
      },
      {
        date: testDay,
        childId: enduserChildFixtureKaarina.id,
        reservationTexts: ['Poissa'],
        childDetails: {
          firstName: enduserChildFixtureKaarina.firstName.split(/\s/)[0],
          lastName: enduserChildFixtureKaarina.lastName,
          preferredName: enduserChildFixtureKaarina.preferredName
        }
      },
      {
        date: testDay,
        childId: enduserChildFixtureJari.id,
        reservationTexts: ['08:12–13:45', '14:30–16:45'],
        childDetails: {
          firstName: enduserChildFixtureJari.firstName.split(/\s/)[0],
          lastName: enduserChildFixtureJari.lastName,
          preferredName: enduserChildFixtureJari.preferredName
        }
      },
      {
        date: testDay,
        childId: enduserChildFixturePorriHatterRestricted.id,
        reservationTexts: ['Ilmoitus puuttuu'],
        childDetails: {
          firstName:
            enduserChildFixturePorriHatterRestricted.firstName.split(/\s/)[0],
          lastName: enduserChildFixturePorriHatterRestricted.lastName,
          preferredName: enduserChildFixturePorriHatterRestricted.preferredName
        }
      }
    ]

    for (const childItem of expectedChildItems) {
      await confirmedReservationPage.assertChildDetails(
        childItem.date,
        childItem.childId,
        childItem.reservationTexts,
        childItem.childDetails
      )
    }
  })

  test('Holiday period daily children are correct (Thursday)', async () => {
    await insertTestHolidayPeriod(
      new FiniteDateRange(LocalDate.of(2022, 5, 19), LocalDate.of(2022, 5, 20))
    )
    const testDay = LocalDate.of(2022, 5, 19)
    await confirmedReservationPage.openDayItem(testDay)
    const expectedChildItems = [
      {
        date: testDay,
        childId: enduserNonSsnChildFixture.id,
        reservationTexts: ['Ei toimintaa'],
        childDetails: {
          firstName: enduserNonSsnChildFixture.firstName.split(/\s/)[0],
          lastName: enduserNonSsnChildFixture.lastName,
          preferredName: enduserNonSsnChildFixture.preferredName
        }
      },
      {
        date: testDay,
        childId: enduserChildFixtureKaarina.id,
        reservationTexts: ['Poissa'],
        childDetails: {
          firstName: enduserChildFixtureKaarina.firstName.split(/\s/)[0],
          lastName: enduserChildFixtureKaarina.lastName,
          preferredName: enduserChildFixtureKaarina.preferredName
        }
      },
      {
        date: testDay,
        childId: enduserChildFixtureJari.id,
        reservationTexts: ['08:12–13:45', '14:30–16:45'],
        childDetails: {
          firstName: enduserChildFixtureJari.firstName.split(/\s/)[0],
          lastName: enduserChildFixtureJari.lastName,
          preferredName: enduserChildFixtureJari.preferredName
        }
      },
      {
        date: testDay,
        childId: enduserChildFixturePorriHatterRestricted.id,
        reservationTexts: ['Lomavaraus puuttuu'],
        childDetails: {
          firstName:
            enduserChildFixturePorriHatterRestricted.firstName.split(/\s/)[0],
          lastName: enduserChildFixturePorriHatterRestricted.lastName,
          preferredName: enduserChildFixturePorriHatterRestricted.preferredName
        }
      }
    ]

    for (const childItem of expectedChildItems) {
      await confirmedReservationPage.assertChildDetails(
        childItem.date,
        childItem.childId,
        childItem.reservationTexts,
        childItem.childDetails
      )
    }
  })
})

async function createPlacements(
  childId: string,
  groupId: string = daycareGroupFixture.id,
  placementType: PlacementType = 'DAYCARE'
) {
  const daycarePlacementFixture = await Fixture.placement()
    .with({
      childId,
      unitId: daycareFixture.id,
      type: placementType,
      startDate: LocalDate.of(2021, 5, 1),
      endDate: LocalDate.of(2022, 8, 31)
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      daycarePlacementId: daycarePlacementFixture.data.id,
      daycareGroupId: groupId,
      startDate: daycarePlacementFixture.data.startDate,
      endDate: daycarePlacementFixture.data.endDate
    })
    .save()
  return daycarePlacementFixture.data
}

async function insertConfirmedDaysTestData() {
  await Fixture.preschoolTerm()
    .with({
      ...preschoolTermFixture2021,
      termBreaks: [
        new FiniteDateRange(
          LocalDate.of(2022, 5, 18),
          LocalDate.of(2022, 5, 20)
        )
      ]
    })
    .save()

  await Fixture.holiday()
    .with({
      date: LocalDate.of(2022, 5, 24),
      description: 'Testing'
    })
    .save()

  const careArea = await Fixture.careArea().with(careAreaFixture).save()
  await Fixture.daycare()
    .with(daycareFixture)
    .careArea(careArea)
    .with({
      shiftCareOperationTimes: null,
      shiftCareOpenOnHolidays: false
    })
    .save()
  await Fixture.daycare().with(daycare2Fixture).careArea(careArea).save()
  await Fixture.daycareGroup().with(daycareGroupFixture).save()
  await Fixture.daycareGroup().with(group2).save()

  await Fixture.person().with(enduserChildFixtureKaarina).save()
  await Fixture.child(enduserChildFixtureKaarina.id).save()

  await Fixture.person().with(enduserChildFixtureJari).save()
  await Fixture.child(enduserChildFixtureJari.id).save()

  await Fixture.person().with(familyWithTwoGuardians.children[0]).save()
  await Fixture.child(familyWithTwoGuardians.children[0].id).save()

  await Fixture.person()
    .with({
      ...enduserChildFixturePorriHatterRestricted,
      dateOfBirth: LocalDate.of(2021, 4, 1)
    })
    .save()
  await Fixture.child(enduserChildFixturePorriHatterRestricted.id).save()

  await Fixture.person().with(enduserNonSsnChildFixture).save()
  await Fixture.child(enduserNonSsnChildFixture.id).save()

  await Fixture.employee()
    .with({ roles: ['ADMIN'] })
    .save()

  await createPlacements(
    enduserChildFixturePorriHatterRestricted.id,
    daycareGroupFixture.id
  )
  await createPlacements(enduserChildFixtureJari.id, daycareGroupFixture.id)
  await createPlacements(enduserChildFixtureKaarina.id, daycareGroupFixture.id)
  await createPlacements(
    enduserNonSsnChildFixture.id,
    daycareGroupFixture.id,
    'PRESCHOOL'
  )

  await Fixture.absence()
    .with({
      childId: enduserChildFixtureKaarina.id,
      date: LocalDate.of(2022, 5, 19)
    })
    .save()
  await Fixture.assistanceFactor()
    .with({
      childId: enduserChildFixtureKaarina.id,
      capacityFactor: 1.5,
      validDuring: new FiniteDateRange(
        LocalDate.of(2022, 5, 27),
        LocalDate.of(2022, 5, 27)
      )
    })
    .save()

  await Fixture.backupCare()
    .with({
      childId: enduserChildFixtureJari.id,
      unitId: daycare2Fixture.id,
      period: new FiniteDateRange(
        LocalDate.of(2022, 5, 26),
        LocalDate.of(2022, 5, 26)
      ),
      groupId: null
    })
    .save()

  await Fixture.backupCare()
    .with({
      childId: enduserChildFixtureJari.id,
      unitId: daycareFixture.id,
      period: new FiniteDateRange(
        LocalDate.of(2022, 5, 27),
        LocalDate.of(2022, 5, 27)
      ),
      groupId: group2.id
    })
    .save()

  await Fixture.attendanceReservation({
    type: 'RESERVATIONS',
    childId: enduserChildFixtureJari.id,
    date: LocalDate.of(2022, 5, 19),
    reservation: new TimeRange(LocalTime.of(8, 12), LocalTime.of(13, 45)),
    secondReservation: new TimeRange(LocalTime.of(14, 30), LocalTime.of(16, 45))
  }).save()
}

async function insertTestHolidayPeriod(period: FiniteDateRange) {
  await Fixture.holidayPeriod()
    .with({ period: period, reservationDeadline: LocalDate.of(2022, 4, 20) })
    .save()
}
