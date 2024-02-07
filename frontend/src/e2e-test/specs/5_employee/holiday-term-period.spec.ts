// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { resetDatabase } from '../../dev-api'
import { Fixture } from '../../dev-api/fixtures'
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

describe('Holiday periods page', () => {
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
})
