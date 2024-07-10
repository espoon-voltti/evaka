// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import { vtjDependants } from '../../dev-api'
import {
  testCareArea,
  testDaycare,
  testDaycareGroup,
  testChild,
  testChild2,
  testAdult,
  Fixture
} from '../../dev-api/fixtures'
import {
  resetServiceState,
  upsertVtjDataset
} from '../../generated/api-clients'
import { DevDaycare, DevPerson } from '../../generated/api-types'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page

const period = new FiniteDateRange(
  LocalDate.of(2035, 12, 18),
  LocalDate.of(2036, 1, 8)
)
const child = testChild
const today = LocalDate.of(2035, 12, 1)
let daycare: DevDaycare
let guardian: DevPerson

const holidayQuestionnaireFixture = () =>
  Fixture.holidayQuestionnaire().with({
    absenceType: 'FREE_ABSENCE',
    active: new FiniteDateRange(
      LocalDate.todayInSystemTz(),
      LocalDate.of(2035, 12, 6)
    ),
    description: {
      en: 'Please submit your reservations for 18.12.2035 - 8.1.2036 asap',
      fi: 'Ystävällisesti pyydän tekemään varauksenne ajalle 18.12.2035 - 8.1.2036 heti kun mahdollista, kuitenkin viimeistään 6.12.',
      sv: 'Vänligen samma på svenska för 18.12.2035 - 8.1.2036'
    },
    periodOptionLabel: {
      en: 'My child is away for 8 weeks between',
      fi: 'Lapseni on poissa 8 viikkoa aikavälillä',
      sv: 'Mitt barn är borta 8 veckor mellan'
    },
    periodOptions: [
      new FiniteDateRange(
        LocalDate.of(2035, 12, 18),
        LocalDate.of(2035, 12, 25)
      ),
      new FiniteDateRange(LocalDate.of(2035, 12, 26), LocalDate.of(2036, 1, 1)),
      new FiniteDateRange(LocalDate.of(2036, 1, 2), LocalDate.of(2036, 1, 8))
    ]
  })

async function assertCalendarDayRange(
  calendar: CitizenCalendarPage,
  startDate: LocalDate,
  endDate: LocalDate,
  groups: { childIds: UUID[]; text: string }[]
) {
  await calendar.waitUntilLoaded()

  let today = startDate
  while (today.isEqualOrBefore(endDate)) {
    await calendar.assertDay(today, groups)
    today = today.addBusinessDays(1)
  }
}

beforeEach(async () => {
  await resetServiceState()
  page = await Page.open({
    mockedTime: today.toHelsinkiDateTime(LocalTime.of(12, 0))
  })

  daycare = await Fixture.daycare()
    .with(testDaycare)
    .careArea(await Fixture.careArea().with(testCareArea).save())
    .save()
  await Fixture.daycareGroup().with(testDaycareGroup).daycare(daycare).save()

  const child1 = await Fixture.person()
    .with(child)
    .saveChild({ updateMockVtj: true })
  guardian = await Fixture.person()
    .with(testAdult)
    .saveAdult({ updateMockVtjWithDependants: [child1] })
  await Fixture.child(child1.id).save()
  await Fixture.guardian(child1, guardian).save()
  await Fixture.placement()
    .child(child1)
    .daycare(daycare)
    .with({
      startDate: LocalDate.of(2022, 1, 1),
      endDate: LocalDate.of(2036, 6, 30)
    })
    .save()
})

async function setupAnotherChild(
  startDate = LocalDate.of(2022, 1, 1),
  endDate = LocalDate.of(2036, 6, 30)
) {
  const child2 = await Fixture.person()
    .with(testChild2)
    .saveChild({ updateMockVtj: true })
  await upsertVtjDataset({ body: vtjDependants(guardian, child2) })
  await Fixture.child(child2.id).save()
  await Fixture.guardian(child2, guardian).save()
  await Fixture.placement()
    .child(child2)
    .daycare(daycare)
    .with({
      startDate: startDate,
      endDate: endDate
    })
    .save()

  return child2
}

describe('Holiday periods and questionnaires', () => {
  describe('Holiday period CTA toast visibility', () => {
    test('No holiday period exists -> no CTA toast', async () => {
      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaNotVisible()
    })
    test('Holiday period with reservations not open yet -> no CTA toast', async () => {
      await Fixture.holidayPeriod()
        .with({
          period,
          reservationsOpenOn: today.addDays(1),
          reservationDeadline: today.addDays(5)
        })
        .save()

      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaNotVisible()
    })
    test('Holiday period with reservations open -> CTA toast is visible', async () => {
      await Fixture.holidayPeriod()
        .with({
          period,
          reservationsOpenOn: today,
          reservationDeadline: today.addDays(5)
        })
        .save()

      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaContent(
        'Ilmoita tästä\n läsnä- ja poissaolot välille 18.12.-08.01.2036 viimeistään 06.12.2035. Läsnäolojen tarkat kellonajat merkitään, kun kysely on päättynyt.'
      )

      await calendar.clickHolidayCta()
      await calendar.reservationModal.waitUntilVisible()
    })
    test('Holiday period with reservations deadline passed -> no CTA toast', async () => {
      await Fixture.holidayPeriod()
        .with({
          period,
          reservationsOpenOn: today.addDays(-2),
          reservationDeadline: today.addDays(-1)
        })
        .save()

      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaNotVisible()
    })
  })

  describe('Holiday questionnaire is active', () => {
    beforeEach(async () => {
      await holidayQuestionnaireFixture().save()
    })

    test('The holiday reservations toast is shown on calendar page', async () => {
      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaContent(
        'Vastaa poissaolokyselyyn 06.12.2035 mennessä.'
      )
    })

    test('Clicking on the holiday reservations toast opens the holiday modal', async () => {
      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.clickHolidayCta()
      await calendar.assertHolidayModalVisible()
    })

    test('The calendar page should show a button for reporting holidays', async () => {
      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayModalButtonVisible()
    })

    test('Holidays can be reported and cleared', async () => {
      const assertFreeAbsences = (hasFreeAbsences: boolean) =>
        assertCalendarDayRange(
          calendar,
          LocalDate.of(2035, 12, 26),
          LocalDate.of(2036, 1, 1),
          [
            {
              childIds: [child.id],
              text: hasFreeAbsences ? 'Maksuton poissaolo' : 'Ilmoitus puuttuu'
            }
          ]
        )

      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')

      await assertFreeAbsences(false)

      let holidayModal = await calendar.openHolidayModal()
      await holidayModal.markHoliday(child, '26.12.2035 - 01.01.2036')

      await assertFreeAbsences(true)

      holidayModal = await calendar.openHolidayModal()
      await holidayModal.markNoHoliday(child)

      await assertFreeAbsences(false)
    })

    test('Holidays can be marked an cleared for two children', async () => {
      const assertFreeAbsences = (hasFreeAbsences: boolean) =>
        assertCalendarDayRange(
          calendar,
          LocalDate.of(2035, 12, 26),
          LocalDate.of(2036, 1, 1),
          [
            {
              childIds: [child.id, child2.id],
              text: hasFreeAbsences ? 'Maksuton poissaolo' : 'Ilmoitus puuttuu'
            }
          ]
        )

      const child2 = await setupAnotherChild()

      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')

      await assertFreeAbsences(false)

      let holidayModal = await calendar.openHolidayModal()
      const selections = [
        { child, option: '26.12.2035 - 01.01.2036' },
        { child: child2, option: '26.12.2035 - 01.01.2036' }
      ]
      await holidayModal.markHolidays(selections)

      await assertFreeAbsences(true)

      const dayView = await calendar.openDayView(LocalDate.of(2035, 12, 26))
      await dayView.assertAbsence(
        child.id,
        'Henkilökunnan merkitsemä poissaolo'
      )
      await dayView.assertAbsence(
        child2.id,
        'Henkilökunnan merkitsemä poissaolo'
      )
      await dayView.close()

      holidayModal = await calendar.openHolidayModal()
      await holidayModal.assertSelectedFixedPeriods(selections)
      await holidayModal.markNoHolidays([child, child2])

      holidayModal = await calendar.openHolidayModal()
      await holidayModal.assertSelectedFixedPeriods(
        selections.map((s) => ({ ...s, option: 'Ei maksutonta poissaoloa' }))
      )

      await assertFreeAbsences(false)
    })
  })

  describe('Holiday questionnaire is inactive', () => {
    beforeEach(async () => {
      await holidayQuestionnaireFixture()
        .with({
          active: new FiniteDateRange(
            LocalDate.of(1990, 1, 1),
            LocalDate.of(1990, 1, 31)
          )
        })
        .save()
    })

    test('The holiday reservations toast is not shown on calendar page', async () => {
      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaNotVisible()
    })
  })

  describe('Child eligibility', () => {
    test('The holiday reservations toast is not shown if no child is eligible', async () => {
      await holidayQuestionnaireFixture()
        .with({
          conditions: {
            continuousPlacement: new FiniteDateRange(
              LocalDate.of(1990, 1, 1),
              LocalDate.of(1990, 1, 31)
            )
          }
        })
        .save()

      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaNotVisible()
    })

    test('Holidays can be marked if one of two children is eligible', async () => {
      const placementConditionStart = LocalDate.of(2022, 1, 1)
      const placementConditionEnd = LocalDate.of(2022, 1, 31)

      await holidayQuestionnaireFixture()
        .with({
          conditions: {
            continuousPlacement: new FiniteDateRange(
              placementConditionStart,
              placementConditionEnd
            )
          }
        })
        .save()

      const child2 = await setupAnotherChild(
        // Not eligible for a free holiday because the placement doesn't cover the required period
        placementConditionStart.addDays(1),
        LocalDate.of(2036, 6, 30)
      )

      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      const holidayModal = await calendar.openHolidayModal()

      await holidayModal.assertNotEligible(child2)
      await holidayModal.markHoliday(child, '26.12.2035 - 01.01.2036')

      let today = LocalDate.of(2035, 12, 26)
      while (today.isEqualOrBefore(LocalDate.of(2036, 1, 1))) {
        await calendar.assertDay(today, [
          { childIds: [child.id], text: 'Maksuton poissaolo' },
          { childIds: [child2.id], text: 'Ilmoitus puuttuu' }
        ])
        today = today.addBusinessDays(1)
      }

      const dayView = await calendar.openDayView(LocalDate.of(2035, 12, 26))
      await dayView.assertAbsence(
        child.id,
        'Henkilökunnan merkitsemä poissaolo'
      )
      await dayView.assertNoReservation(child2.id)
    })
  })
})
