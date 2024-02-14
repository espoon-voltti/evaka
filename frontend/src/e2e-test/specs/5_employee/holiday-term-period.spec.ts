// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetDatabase } from '../../generated/api-clients'
import EmployeeNav from '../../pages/employee/employee-nav'
import { HolidayAndTermPeriodsPage } from '../../pages/employee/holiday-term-periods'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let holidayAndTermPeriodsPage: HolidayAndTermPeriodsPage

beforeEach(async () => {
  await resetDatabase()
  const admin = await Fixture.employeeAdmin().save()
  page = await Page.open({
    mockedTime: LocalDate.of(2021, 11, 1).toHelsinkiDateTime(
      LocalTime.of(12, 0)
    )
  })
  await employeeLogin(page, admin.data)
  await page.goto(config.employeeUrl)
  holidayAndTermPeriodsPage = new HolidayAndTermPeriodsPage(page)
})

describe('Holiday and term periods page', () => {
  beforeEach(async () => {
    await new EmployeeNav(page).openAndClickDropdownMenuItem('holiday-periods')
  })

  test('Holiday periods can be created, updated and deleted', async () => {
    await holidayAndTermPeriodsPage.clickAddPeriodButton()
    await holidayAndTermPeriodsPage.fillHolidayPeriodForm({
      start: '15.12.2021',
      end: '31.12.2021',
      reservationDeadline: '7.12.2021'
    })
    await holidayAndTermPeriodsPage.confirmCheckbox.check()
    await holidayAndTermPeriodsPage.submit()
    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visiblePeriods,
      ['15.12.2021 - 31.12.2021']
    )

    await holidayAndTermPeriodsPage.clickAddPeriodButton()
    await holidayAndTermPeriodsPage.fillHolidayPeriodForm({
      start: '1.2.2022',
      end: '7.2.2022',
      reservationDeadline: '15.1.2022'
    })
    await holidayAndTermPeriodsPage.confirmCheckbox.check()
    await holidayAndTermPeriodsPage.submit()
    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visiblePeriods,
      ['15.12.2021 - 31.12.2021', '01.02.2022 - 07.02.2022']
    )

    await holidayAndTermPeriodsPage.editHolidayPeriod(0)
    await holidayAndTermPeriodsPage.fillHolidayPeriodForm({ end: '6.1.2022' })
    await holidayAndTermPeriodsPage.submit()
    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visiblePeriods,
      ['15.12.2021 - 06.01.2022', '01.02.2022 - 07.02.2022']
    )

    await holidayAndTermPeriodsPage.deleteHolidayPeriod(0)
    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visiblePeriods,
      ['01.02.2022 - 07.02.2022']
    )
  })

  test('Holiday questionnaires can be created, updated and deleted', async () => {
    await holidayAndTermPeriodsPage.clickAddPeriodButton()
    await holidayAndTermPeriodsPage.fillHolidayPeriodForm({
      start: '15.12.2021',
      end: '31.12.2021',
      reservationDeadline: '7.12.2021'
    })
    await holidayAndTermPeriodsPage.confirmCheckbox.check()
    await holidayAndTermPeriodsPage.submit()

    await holidayAndTermPeriodsPage.clickAddQuestionnaireButton()
    await holidayAndTermPeriodsPage.fillQuestionnaireForm({
      activeStart: '15.2.2022',
      activeEnd: '3.5.2022',
      title: '8 viikon maksuton jakso',
      description:
        'Pyydämme ilmoittamaan 3.5. mennessä lapsenne kesälomat. Jos lapsi on ennalta ilmoitetusti yhtenäisesti poissa 8 viikon ajan 31.5.–29.8. välillä, niin asiakasmaksu hyvitetään kesä- ja heinäkuulta.',
      fixedPeriodOptions: '30.05.2022 - 31.5.2022, 30.6.2022-31.7.2022',
      fixedPeriodOptionLabel: 'Lapsi on poissa 8 viikkoa aikavälillä'
    })

    await holidayAndTermPeriodsPage.submit()

    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visibleQuestionnaires,
      [['15.02.2022 - 03.05.2022', '8 viikon maksuton jakso', ''].join('\t')]
    )

    await holidayAndTermPeriodsPage.editQuestionnaire(0)
    await holidayAndTermPeriodsPage.fillQuestionnaireForm({
      title: '6 viikon loma'
    })
    await holidayAndTermPeriodsPage.submit()
    await holidayAndTermPeriodsPage.assertQuestionnaireContainsText(0, [
      '6 viikon loma'
    ])

    await holidayAndTermPeriodsPage.deleteQuestionnaire(0)
    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visibleQuestionnaires,
      []
    )
  })

  test('Preschool terms can be created, updated and deleted', async () => {
    await holidayAndTermPeriodsPage.clickAddPreschoolTermButton()

    const firstTermBreaks = [
      new FiniteDateRange(
        LocalDate.of(2021, 12, 23),
        LocalDate.of(2021, 12, 27)
      ),
      new FiniteDateRange(LocalDate.of(2022, 2, 1), LocalDate.of(2022, 2, 10))
    ]

    await holidayAndTermPeriodsPage.fillPreschoolTermForm({
      finnishPreschoolStart: '01.08.2021',
      finnishPreschoolEnd: '30.05.2022',
      extendedTermStart: '01.07.2021',
      applicationPeriodStart: '01.06.2021',
      termBreaks: firstTermBreaks
    })
    await holidayAndTermPeriodsPage.submit()

    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visiblePreschoolTermPeriods,
      ['01.08.2021 - 30.05.2022']
    )
    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visibleExtendedTermStartDates,
      ['01.07.2021']
    )
    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visibleApplicationPeriodStartDates,
      ['01.06.2021']
    )

    for (const tb of firstTermBreaks) {
      await waitUntilEqual(
        () => holidayAndTermPeriodsPage.visibleTermBreakByDate(tb.start),
        [tb.formatCompact()]
      )
    }

    await holidayAndTermPeriodsPage.clickAddPreschoolTermButton()

    const secondTermBreaks = [
      new FiniteDateRange(
        LocalDate.of(2025, 12, 23),
        LocalDate.of(2025, 12, 27)
      ),
      new FiniteDateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10))
    ]
    await holidayAndTermPeriodsPage.fillPreschoolTermForm({
      finnishPreschoolStart: '01.08.2025',
      finnishPreschoolEnd: '30.05.2026',
      extendedTermStart: '01.07.2025',
      applicationPeriodStart: '01.06.2025',
      termBreaks: secondTermBreaks
    })
    await holidayAndTermPeriodsPage.submit()
    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visiblePreschoolTermPeriods,
      ['01.08.2025 - 30.05.2026', '01.08.2021 - 30.05.2022']
    )
    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visibleExtendedTermStartDates,
      ['01.07.2025', '01.07.2021']
    )
    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visibleApplicationPeriodStartDates,
      ['01.06.2025', '01.06.2021']
    )

    for (const tb of secondTermBreaks) {
      await waitUntilEqual(
        () => holidayAndTermPeriodsPage.visibleTermBreakByDate(tb.start),
        [tb.formatCompact()]
      )
    }

    // Edit first row
    await holidayAndTermPeriodsPage.editPreschoolTerm(0)
    const updatedTermBreaks = [
      new FiniteDateRange(LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 15))
    ]
    await holidayAndTermPeriodsPage.fillPreschoolTermForm({
      finnishPreschoolStart: '01.07.2025',
      finnishPreschoolEnd: '30.04.2026',
      extendedTermStart: '01.06.2025',
      applicationPeriodStart: '01.05.2025'
    })

    await holidayAndTermPeriodsPage.removeTermBreakEntry(1)
    await holidayAndTermPeriodsPage.editTermBreakInput(0, updatedTermBreaks[0])

    await holidayAndTermPeriodsPage.submit()

    // Edit second row, should open modal because it has started before current mocked time
    await holidayAndTermPeriodsPage.editPreschoolTerm(1)

    await holidayAndTermPeriodsPage.confirmPreschoolTermModal()

    await holidayAndTermPeriodsPage.fillPreschoolTermForm({
      finnishPreschoolStart: '10.08.2021',
      finnishPreschoolEnd: '01.06.2022',
      extendedTermStart: '01.08.2021',
      applicationPeriodStart: '01.05.2021'
    })

    await holidayAndTermPeriodsPage.submit()

    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visiblePreschoolTermPeriods,
      ['01.07.2025 - 30.04.2026', '10.08.2021 - 01.06.2022']
    )
    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visibleExtendedTermStartDates,
      ['01.06.2025', '01.08.2021']
    )
    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visibleApplicationPeriodStartDates,
      ['01.05.2025', '01.05.2021']
    )

    for (const tb of updatedTermBreaks) {
      await waitUntilEqual(
        () => holidayAndTermPeriodsPage.visibleTermBreakByDate(tb.start),
        [tb.formatCompact()]
      )
    }

    for (const tb of firstTermBreaks) {
      await waitUntilEqual(
        () => holidayAndTermPeriodsPage.visibleTermBreakByDate(tb.start),
        [tb.formatCompact()]
      )
    }

    // Delete latest row
    await holidayAndTermPeriodsPage.deletePreschoolTerm(0)

    await holidayAndTermPeriodsPage.confirmPreschoolTermModal()

    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visiblePreschoolTermPeriods,
      ['10.08.2021 - 01.06.2022']
    )
    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visibleExtendedTermStartDates,
      ['01.08.2021']
    )
    await waitUntilEqual(
      () => holidayAndTermPeriodsPage.visibleApplicationPeriodStartDates,
      ['01.05.2021']
    )

    for (const tb of firstTermBreaks) {
      await waitUntilEqual(
        () => holidayAndTermPeriodsPage.visibleTermBreakByDate(tb.start),
        [tb.formatCompact()]
      )
    }
  })
})
