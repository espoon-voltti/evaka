// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
import { resetDatabase } from '../../dev-api'
import { Fixture } from '../../dev-api/fixtures'
import EmployeeNav from '../../pages/employee/employee-nav'
import { HolidayPeriodsPage } from '../../pages/employee/holiday-periods'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let holidayPeriodsPage: HolidayPeriodsPage

beforeEach(async () => {
  await resetDatabase()
  const admin = await Fixture.employeeAdmin().save()
  page = await Page.open()
  await employeeLogin(page, admin.data)
  await page.goto(config.employeeUrl)
  holidayPeriodsPage = new HolidayPeriodsPage(page)
})

describe('Holiday periods page', () => {
  beforeEach(async () => {
    await new EmployeeNav(page).openAndClickDropdownMenuItem('holiday-periods')
  })

  test('Holiday periods can be created, updated and deleted', async () => {
    await holidayPeriodsPage.clickAddPeriodButton()
    await holidayPeriodsPage.fillHolidayPeriodForm({
      start: LocalDate.of(2021, 12, 15),
      end: LocalDate.of(2021, 12, 31),
      reservationDeadline: LocalDate.of(2021, 12, 7)
    })
    await holidayPeriodsPage.submit()
    await waitUntilEqual(
      () => holidayPeriodsPage.visiblePeriods,
      ['15.12.2021 - 31.12.2021']
    )

    await holidayPeriodsPage.clickAddPeriodButton()
    await holidayPeriodsPage.fillHolidayPeriodForm({
      start: LocalDate.of(2022, 2, 1),
      end: LocalDate.of(2022, 2, 7),
      reservationDeadline: LocalDate.of(2022, 1, 15)
    })
    await holidayPeriodsPage.submit()
    await waitUntilEqual(
      () => holidayPeriodsPage.visiblePeriods,
      ['15.12.2021 - 31.12.2021', '01.02.2022 - 07.02.2022']
    )

    await holidayPeriodsPage.editHolidayPeriod(0)
    await holidayPeriodsPage.fillHolidayPeriodForm({
      end: LocalDate.of(2022, 1, 6)
    })
    await holidayPeriodsPage.submit()
    await waitUntilEqual(
      () => holidayPeriodsPage.visiblePeriods,
      ['15.12.2021 - 06.01.2022', '01.02.2022 - 07.02.2022']
    )

    await holidayPeriodsPage.deleteHolidayPeriod(0)
    await waitUntilEqual(
      () => holidayPeriodsPage.visiblePeriods,
      ['01.02.2022 - 07.02.2022']
    )
  })

  test('Holiday questionnaires can be created, updated and deleted', async () => {
    await holidayPeriodsPage.clickAddPeriodButton()
    await holidayPeriodsPage.fillHolidayPeriodForm({
      start: LocalDate.of(2021, 12, 15),
      end: LocalDate.of(2021, 12, 31),
      reservationDeadline: LocalDate.of(2021, 12, 7)
    })
    await holidayPeriodsPage.submit()

    await holidayPeriodsPage.clickAddQuestionnaireButton()
    await holidayPeriodsPage.fillQuestionnaireForm({
      activeRange: new FiniteDateRange(
        LocalDate.of(2022, 2, 15),
        LocalDate.of(2022, 5, 3)
      ),
      title: '8 viikon maksuton jakso',
      description:
        'Pyydämme ilmoittamaan 3.5. mennessä lapsenne kesälomat. Jos lapsi on ennalta ilmoitetusti yhtenäisesti poissa 8 viikon ajan 31.5.–29.8. välillä, niin asiakasmaksu hyvitetään kesä- ja heinäkuulta.',
      fixedPeriodOptions: '30.05.2022 - 31.5.2022, 30.6.2022-31.7.2022',
      fixedPeriodOptionLabel: 'Lapsi on poissa 8 viikkoa aikavälillä'
    })

    await holidayPeriodsPage.submit()

    await waitUntilEqual(
      () => holidayPeriodsPage.visibleQuestionnaires,
      [['15.02.2022 - 03.05.2022', '8 viikon maksuton jakso', ''].join('\t')]
    )

    await holidayPeriodsPage.editQuestionnaire(0)
    await holidayPeriodsPage.fillQuestionnaireForm({ title: '6 viikon loma' })
    await holidayPeriodsPage.submit()
    await holidayPeriodsPage.assertQuestionnaireContainsText(0, [
      '6 viikon loma'
    ])

    await holidayPeriodsPage.deleteQuestionnaire(0)
    await waitUntilEqual(() => holidayPeriodsPage.visibleQuestionnaires, [])
  })
})
