// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
    await holidayPeriodsPage.clickAddButton()
    await holidayPeriodsPage.fillForm({
      description: 'Varaathan hoito-aikasi joulun ajalle ajoissa (15.-31.12.)',
      descriptionLink: 'https://example.com',
      start: '15.12.2021',
      end: '31.12.2021',
      reservationDeadline: '7.12.2021',
      showReservationBannerFrom: '27.11.2021'
    })
    await holidayPeriodsPage.submit()
    await waitUntilEqual(
      () => holidayPeriodsPage.visiblePeriods,
      ['15.12.2021 - 31.12.2021']
    )

    await holidayPeriodsPage.clickAddButton()
    await holidayPeriodsPage.fillForm({
      description: 'Merkatkaa hiihtolomat 15.1. mennessä!',
      descriptionLink: '',
      start: '1.2.2022',
      end: '7.2.2022',
      reservationDeadline: '15.1.2022',
      showReservationBannerFrom: '1.1.2022'
    })
    await holidayPeriodsPage.submit()
    await waitUntilEqual(
      () => holidayPeriodsPage.visiblePeriods,
      ['15.12.2021 - 31.12.2021', '01.02.2022 - 07.02.2022']
    )

    await holidayPeriodsPage.editHolidayPeriod(0)
    await holidayPeriodsPage.fillForm({ end: '6.1.2022' })
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

  test('Holiday periods can be saved for each language', async () => {
    await holidayPeriodsPage.clickAddButton()
    const descriptions = {
      description: 'Finnish text',
      descriptionSv: 'Svenska text',
      descriptionEn: 'English text',
      descriptionLink: 'https://www.example.com/fi',
      descriptionLinkSv: 'https://www.example.com/sv',
      descriptionLinkEn: 'https://www.example.com/en'
    }
    await holidayPeriodsPage.fillForm({
      start: '1.1.2021',
      end: '2.2.2021',
      reservationDeadline: '1.1.2021',
      showReservationBannerFrom: '1.1.2021',
      ...descriptions
    })
    await holidayPeriodsPage.submit()

    await waitUntilEqual(
      () => holidayPeriodsPage.visiblePeriods,
      ['01.01.2021 - 02.02.2021']
    )

    await holidayPeriodsPage.assertRowContainsText(
      0,
      Object.values(descriptions)
    )
  })
})
