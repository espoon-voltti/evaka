// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import LocalDate from 'lib-common/local-date'

import {
  insertDaycarePlacementFixtures,
  insertServiceNeeds,
  resetDatabase
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { Daycare, PersonDetail } from '../../dev-api/types'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const e: ('desktop' | 'mobile')[] = ['desktop', 'mobile']
describe.each(e)('Citizen attendance reservations (%s)', (env) => {
  let fixtures: AreaAndPersonFixtures
  const today = LocalDate.of(2022, 1, 5)
  const firstReservationDay = today.addDays(14)

  beforeEach(async () => {
    await resetDatabase()
    fixtures = await initializeAreaAndPersonData()
  })

  const insertTestChild = async (
    child: PersonDetail,
    serviceNeedOption: ServiceNeedOption,
    unit: Daycare = fixtures.daycareFixture
  ) => {
    const placement = createDaycarePlacementFixture(
      uuidv4(),
      child.id,
      unit.id,
      today.formatIso(),
      today.addYears(1).formatIso()
    )
    await insertDaycarePlacementFixtures([placement])
    const group = await Fixture.daycareGroup()
      .with({ daycareId: unit.id })
      .save()
    await Fixture.groupPlacement()
      .withGroup(group)
      .with({
        daycarePlacementId: placement.id,
        startDate: placement.startDate,
        endDate: placement.endDate
      })
      .save()
    const employee = await Fixture.employeeStaff(unit.id)
      .save()
      .then((e) => e.data)
    await insertServiceNeeds([
      {
        id: uuidv4(),
        placementId: placement.id,
        startDate: placement.startDate,
        endDate: placement.endDate,
        optionId: serviceNeedOption.id,
        shiftCare: false,
        confirmedBy: employee.id,
        confirmedAt: today
      }
    ])
  }

  async function openCalendarPage() {
    const viewport =
      env === 'mobile'
        ? { width: 375, height: 812 }
        : { width: 1920, height: 1080 }

    const page = await Page.open({
      viewport,
      screen: viewport,
      mockedTime: today.toSystemTzDate(),
      citizenCustomizations: {
        featureFlags: {
          citizenShiftCareAbsence: false,
          citizenContractDayAbsence: true
        }
      }
    })
    await enduserLogin(page)
    const header = new CitizenHeader(page, env)
    await header.selectTab('calendar')
    return new CitizenCalendarPage(page, env)
  }

  test("contract day absence type is not visible when children doesn't have correct service need", async () => {
    const snContractDaysPerMonthNull = await Fixture.serviceNeedOption()
      .with({ contractDaysPerMonth: null })
      .save()
      .then((builder) => builder.data)
    await insertTestChild(
      fixtures.enduserChildFixtureJari,
      snContractDaysPerMonthNull
    )

    const calendarPage = await openCalendarPage()
    const absencesModal = await calendarPage.openAbsencesModal()
    await absencesModal.selectDates(
      new FiniteDateRange(firstReservationDay, firstReservationDay)
    )
    await absencesModal.getAbsenceChip('PLANNED_ABSENCE').waitUntilHidden()

    // verify sickleave works from today
    await absencesModal.selectDates(new FiniteDateRange(today, today))
    await absencesModal.selectAbsenceType('SICKLEAVE')
    await absencesModal.submit()
    await calendarPage.assertReservations(today, [
      {
        absence: true,
        childIds: [fixtures.enduserChildFixtureJari.id]
      }
    ])
  })

  test('contract day absence type is visible when some of the children does have correct service need', async () => {
    const snContractDaysPerMonth10 = await Fixture.serviceNeedOption()
      .with({ contractDaysPerMonth: 10 })
      .save()
      .then((builder) => builder.data)
    const snContractDaysPerMonthNull = await Fixture.serviceNeedOption()
      .with({ contractDaysPerMonth: null })
      .save()
      .then((builder) => builder.data)
    await insertTestChild(
      fixtures.enduserChildFixtureJari,
      snContractDaysPerMonth10
    )
    await insertTestChild(
      fixtures.enduserChildFixtureKaarina,
      snContractDaysPerMonthNull
    )
    await insertTestChild(
      fixtures.enduserChildFixturePorriHatterRestricted,
      snContractDaysPerMonth10
    )

    const calendarPage = await openCalendarPage()
    const absencesModal = await calendarPage.openAbsencesModal()
    await absencesModal.selectDates(
      new FiniteDateRange(firstReservationDay, firstReservationDay)
    )
    await absencesModal.getAbsenceChip('PLANNED_ABSENCE').waitUntilVisible()
    await absencesModal.selectAbsenceType('PLANNED_ABSENCE')
    await absencesModal.submit()
    await absencesModal.getAbsenceTypeRequiredError().waitUntilVisible()

    // verify planned absence works when all selected children have contract days
    await absencesModal.toggleChildren([fixtures.enduserChildFixtureKaarina])
    await absencesModal.selectAbsenceType('PLANNED_ABSENCE')
    await absencesModal.submit()
    await calendarPage.assertReservations(firstReservationDay, [
      {
        absence: true,
        childIds: [
          fixtures.enduserChildFixtureJari.id,
          fixtures.enduserChildFixturePorriHatterRestricted.id
        ]
      }
    ])
  })
})
