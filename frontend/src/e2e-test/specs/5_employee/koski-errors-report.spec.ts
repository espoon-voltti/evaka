// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevEmployee, DevPerson } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage from '../../pages/employee/reports'
import { test } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedToday = LocalDate.of(2024, 6, 3)

test.describe('Koski errors report', () => {
  test.use({
    evakaOptions: {
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    }
  })

  let admin: DevEmployee
  let age7: DevPerson
  let turnedEightToday: DevPerson
  let turnsNineTomorrow: DevPerson
  let turnedNineToday: DevPerson

  test.beforeEach(async () => {
    await resetServiceState()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({ areaId: area.id }).save()

    // Both edges of age 8 are shown by default; the first child aged 9 is not
    age7 = await Fixture.person({
      dateOfBirth: mockedToday.subYears(8).addDays(1)
    }).saveChild()
    turnedEightToday = await Fixture.person({
      dateOfBirth: mockedToday.subYears(8)
    }).saveChild()
    turnsNineTomorrow = await Fixture.person({
      dateOfBirth: mockedToday.subYears(9).addDays(1)
    }).saveChild()
    turnedNineToday = await Fixture.person({
      dateOfBirth: mockedToday.subYears(9)
    }).saveChild()

    for (const child of [
      age7,
      turnedEightToday,
      turnsNineTomorrow,
      turnedNineToday
    ]) {
      await Fixture.koskiUploadError({
        childId: child.id,
        unitId: unit.id
      }).save()
    }

    admin = await Fixture.employee().admin().save()
  })

  test('children over 8 are hidden unless included', async ({ evaka }) => {
    const report = await navigateToReport(evaka, admin)

    await report.assertChildren([
      age7.id,
      turnedEightToday.id,
      turnsNineTomorrow.id
    ])

    await report.includeOver8y.check()
    await report.assertChildren([
      age7.id,
      turnedEightToday.id,
      turnsNineTomorrow.id,
      turnedNineToday.id
    ])
  })

  const navigateToReport = async (page: Page, user: DevEmployee) => {
    await employeeLogin(page, user)
    await page.goto(config.employeeUrl)
    await new EmployeeNav(page).openTab('reports')
    return await new ReportsPage(page).openKoskiErrorsReport()
  }
})
