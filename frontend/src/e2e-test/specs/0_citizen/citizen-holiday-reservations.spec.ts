// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import { HolidayQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import type { UUID } from 'lib-common/types'

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
import type {
  DevDaycare,
  DevPerson,
  DevPlacement
} from '../../generated/api-types'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

import FixedPeriodQuestionnaire = HolidayQuestionnaire.FixedPeriodQuestionnaire

let page: Page

const period = new FiniteDateRange(
  LocalDate.of(2035, 12, 18),
  LocalDate.of(2036, 1, 8)
)
const child = testChild
const today = LocalDate.of(2035, 12, 1)
let daycare: DevDaycare
let guardian: DevPerson

const holidayQuestionnaireFixture = (
  initial?: Partial<FixedPeriodQuestionnaire>
) =>
  Fixture.holidayQuestionnaire({
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
    ],
    ...initial
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

  const area = await testCareArea.save()
  daycare = await Fixture.daycare({ ...testDaycare, areaId: area.id }).save()
  await Fixture.daycareGroup({
    ...testDaycareGroup,
    daycareId: daycare.id
  }).save()

  const child1 = await child.saveChild({ updateMockVtj: true })
  guardian = await testAdult.saveAdult({
    updateMockVtjWithDependants: [child1]
  })
  await Fixture.guardian(child1, guardian).save()
})
async function setupFirstChildPlacement(initial?: Partial<DevPlacement>) {
  await Fixture.placement({
    childId: child.id,
    unitId: daycare.id,
    startDate: LocalDate.of(2022, 1, 1),
    endDate: LocalDate.of(2036, 6, 30),
    ...initial
  }).save()
}

async function setupAnotherChild(
  startDate = LocalDate.of(2022, 1, 1),
  endDate = LocalDate.of(2036, 6, 30)
) {
  const child2 = await testChild2.saveChild({
    updateMockVtj: true
  })
  await upsertVtjDataset({ body: vtjDependants(guardian, child2) })
  await Fixture.guardian(child2, guardian).save()
  await Fixture.placement({
    childId: child2.id,
    unitId: daycare.id,
    startDate: startDate,
    endDate: endDate
  }).save()

  return child2
}

describe('Holiday periods and questionnaires', () => {
  describe('Holiday period CTA toast visibility', () => {
    beforeEach(async () => {
      await setupFirstChildPlacement()
    })
    test('No holiday period exists -> no CTA toast', async () => {
      await enduserLogin(page, guardian)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaNotVisible()
    })
    test('Holiday period with reservations not open yet -> no CTA toast', async () => {
      await Fixture.holidayPeriod({
        period,
        reservationsOpenOn: today.addDays(1),
        reservationDeadline: today.addDays(5)
      }).save()

      await enduserLogin(page, guardian)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaNotVisible()
    })
    test('Holiday period with reservations open -> CTA toast is visible', async () => {
      await Fixture.holidayPeriod({
        period,
        reservationsOpenOn: today,
        reservationDeadline: today.addDays(5)
      }).save()

      await enduserLogin(page, guardian)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaContent(
        'Ilmoita tästä\n läsnä- ja poissaolot välille 18.12.-08.01.2036 viimeistään 06.12.2035. Läsnäolojen tarkat kellonajat merkitään, kun kysely on päättynyt.'
      )

      await calendar.clickHolidayCta()
      await calendar.reservationModal.waitUntilVisible()
    })
    test('Holiday period with reservations deadline passed -> no CTA toast', async () => {
      await Fixture.holidayPeriod({
        period,
        reservationsOpenOn: today.addDays(-2),
        reservationDeadline: today.addDays(-1)
      }).save()

      await enduserLogin(page, guardian)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaNotVisible()
    })
  })

  describe('Holiday questionnaire is active', () => {
    beforeEach(async () => {
      await setupFirstChildPlacement()
      await holidayQuestionnaireFixture().save()
    })

    test('The holiday reservations toast is shown on calendar page', async () => {
      await enduserLogin(page, guardian)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaContent(
        'Vastaa poissaolokyselyyn 06.12.2035 mennessä.'
      )
    })

    test('The holiday reservations toast is hidden after answering questionnaire', async () => {
      await enduserLogin(page, guardian)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaContent(
        'Vastaa poissaolokyselyyn 06.12.2035 mennessä.'
      )
      const holidayModal = await calendar.openHolidayModal()
      await holidayModal.markNoHoliday(child)
      await calendar.assertHolidayCtaNotVisible()
    })

    test('Clicking on the holiday reservations toast opens the holiday modal', async () => {
      await enduserLogin(page, guardian)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.clickHolidayCta()
      await calendar.assertHolidayModalVisible()
    })

    test('The calendar page should show a button for reporting holidays', async () => {
      await enduserLogin(page, guardian)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayModalButtonVisible()
    })

    test('Holidays can be reported and cleared', async () => {
      const assertFreeAbsences = (hasFreeAbsences: boolean) =>
        assertCalendarDayRange(
          calendar,
          LocalDate.of(2035, 12, 27),
          LocalDate.of(2035, 12, 31),
          [
            {
              childIds: [child.id],
              text: hasFreeAbsences ? 'Maksuton poissaolo' : 'Ilmoitus puuttuu'
            }
          ]
        )

      await enduserLogin(page, guardian)
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
          LocalDate.of(2035, 12, 27),
          LocalDate.of(2035, 12, 31),
          [
            {
              childIds: [child.id, child2.id],
              text: hasFreeAbsences ? 'Maksuton poissaolo' : 'Ilmoitus puuttuu'
            }
          ]
        )

      const child2 = await setupAnotherChild()

      await enduserLogin(page, guardian)
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

      const dayView = await calendar.openDayView(LocalDate.of(2035, 12, 27))
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
      await setupFirstChildPlacement()
      await holidayQuestionnaireFixture({
        active: new FiniteDateRange(
          LocalDate.of(1990, 1, 1),
          LocalDate.of(1990, 1, 31)
        )
      }).save()
    })

    test('The holiday reservations toast is not shown on calendar page', async () => {
      await enduserLogin(page, guardian)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaNotVisible()
    })
  })

  describe('Child eligibility', () => {
    test('The holiday reservations toast is not shown if no child is eligible', async () => {
      await setupFirstChildPlacement()
      await holidayQuestionnaireFixture({
        conditions: {
          continuousPlacement: new FiniteDateRange(
            LocalDate.of(1990, 1, 1),
            LocalDate.of(1990, 1, 31)
          )
        }
      }).save()

      await enduserLogin(page, guardian)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaNotVisible()
    })
    test('The holiday reservations toast is not shown if no child is eligible: placement ends after active period', async () => {
      // today is LocalDate.of(2035, 12, 1)
      await setupFirstChildPlacement({
        startDate: LocalDate.of(2022, 1, 1),
        endDate: LocalDate.of(2035, 12, 17)
      })
      await setupAnotherChild(
        LocalDate.of(1990, 1, 1),
        LocalDate.of(1991, 12, 17)
      )

      await holidayQuestionnaireFixture({
        conditions: {
          continuousPlacement: null
        },
        active: new FiniteDateRange(
          LocalDate.todayInSystemTz(),
          LocalDate.of(2035, 12, 6)
        ),
        periodOptions: [
          new FiniteDateRange(
            LocalDate.of(2035, 12, 18),
            LocalDate.of(2035, 12, 25)
          )
        ]
      }).save()

      await enduserLogin(page, guardian)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaNotVisible()
    })

    test('The holiday reservations toast is not shown if no child is eligible: placement is non billable', async () => {
      await setupFirstChildPlacement({
        startDate: LocalDate.of(2035, 12, 1),
        endDate: LocalDate.of(2035, 12, 31),
        type: 'CLUB'
      })

      await holidayQuestionnaireFixture({
        conditions: {
          continuousPlacement: null
        },
        active: new FiniteDateRange(
          LocalDate.todayInSystemTz(),
          LocalDate.of(2035, 12, 6)
        ),
        periodOptions: [
          new FiniteDateRange(
            LocalDate.of(2035, 12, 18),
            LocalDate.of(2035, 12, 25)
          )
        ]
      }).save()

      await enduserLogin(page, guardian)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayCtaNotVisible()
    })

    test('Holidays can be marked if one of two children is eligible', async () => {
      await setupFirstChildPlacement()
      const placementConditionStart = LocalDate.of(2022, 1, 1)
      const placementConditionEnd = LocalDate.of(2022, 1, 31)

      await holidayQuestionnaireFixture({
        conditions: {
          continuousPlacement: new FiniteDateRange(
            placementConditionStart,
            placementConditionEnd
          )
        }
      }).save()

      const child2 = await setupAnotherChild(
        // Not eligible for a free holiday because the placement doesn't cover the required period
        placementConditionStart.addDays(1),
        LocalDate.of(2036, 6, 30)
      )

      await enduserLogin(page, guardian)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      const holidayModal = await calendar.openHolidayModal()

      await holidayModal.assertNotEligible(child2)
      await holidayModal.markHoliday(child, '26.12.2035 - 01.01.2036')

      let today = LocalDate.of(2035, 12, 27)
      while (today.isEqualOrBefore(LocalDate.of(2035, 12, 31))) {
        await calendar.assertDay(today, [
          { childIds: [child.id], text: 'Maksuton poissaolo' },
          { childIds: [child2.id], text: 'Ilmoitus puuttuu' }
        ])
        today = today.addBusinessDays(1)
      }

      const dayView = await calendar.openDayView(LocalDate.of(2035, 12, 27))
      await dayView.assertAbsence(
        child.id,
        'Henkilökunnan merkitsemä poissaolo'
      )
      await dayView.assertNoReservation(child2.id)
    })
  })

  test('Holiday period options are selectable based on placement duration', async () => {
    await setupFirstChildPlacement()
    await holidayQuestionnaireFixture().save()
    const child2 = await setupAnotherChild(
      LocalDate.of(2035, 12, 19),
      LocalDate.of(2036, 1, 1)
    )

    await enduserLogin(page, guardian)
    await new CitizenHeader(page).selectTab('calendar')
    const calendar = new CitizenCalendarPage(page, 'desktop')
    const holidayModal = await calendar.openHolidayModal()

    await holidayModal.assertOptions(child, [
      'Ei maksutonta poissaoloa',
      '18.12.2035 - 25.12.2035',
      '26.12.2035 - 01.01.2036',
      '02.01.2036 - 08.01.2036'
    ])
    await holidayModal.assertOptions(child2, [
      'Ei maksutonta poissaoloa',
      '26.12.2035 - 01.01.2036'
    ])
    await holidayModal.markHolidays([
      {
        child,
        option: '02.01.2036 - 08.01.2036'
      },
      {
        child: child2,
        option: '26.12.2035 - 01.01.2036'
      }
    ])

    await calendar.assertDay(LocalDate.of(2035, 12, 27), [
      { childIds: [child.id], text: 'Ilmoitus puuttuu' },
      { childIds: [child2.id], text: 'Maksuton poissaolo' }
    ])
    await calendar.assertDay(LocalDate.of(2036, 1, 2), [
      { childIds: [child.id], text: 'Maksuton poissaolo' }
    ])

    const dayView1 = await calendar.openDayView(LocalDate.of(2035, 12, 27))
    await dayView1.assertNoReservation(child.id)
    await dayView1.assertAbsence(
      child2.id,
      'Henkilökunnan merkitsemä poissaolo'
    )
    await dayView1.close()
    const dayView2 = await calendar.openDayView(LocalDate.of(2036, 1, 2))
    await dayView2.assertAbsence(child.id, 'Henkilökunnan merkitsemä poissaolo')
    await dayView2.close()
  })
})
