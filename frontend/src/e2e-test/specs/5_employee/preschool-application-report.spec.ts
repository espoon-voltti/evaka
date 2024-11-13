// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { applicationFixture, Fixture, uuidv4 } from '../../dev-api/fixtures'
import {
  createApplications,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedToday = LocalDate.of(2024, 10, 16)

beforeEach(resetServiceState)

xdescribe('Preschool application report', () => {
  test('no results is shown', async () => {
    const admin = await Fixture.employee().admin().save()
    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })
    const report = await navigateToReport(page, admin)
    await report.assertNoResults()
  })

  test('rows are shown', async () => {
    const nextTermStart = LocalDate.of(2025, 8, 6)
    const nextTermRange = new FiniteDateRange(
      nextTermStart,
      LocalDate.of(2026, 5, 29)
    )
    await Fixture.preschoolTerm({
      finnishPreschool: nextTermRange,
      swedishPreschool: nextTermRange,
      extendedTerm: nextTermRange,
      applicationPeriod: nextTermRange,
      termBreaks: []
    }).save()
    const area = await Fixture.careArea().save()
    const unit1 = await Fixture.daycare({
      areaId: area.id,
      name: 'Koulu A'
    }).save()
    const unit2 = await Fixture.daycare({
      areaId: area.id,
      name: 'Koulu B'
    }).save()

    const guardian = await Fixture.person({
      lastName: 'Testiläinen',
      firstName: 'Matti',
      dateOfBirth: LocalDate.of(2000, 1, 1),
      ssn: null
    }).saveAdult()

    const child1 = await Fixture.person({
      lastName: 'Testiläinen',
      firstName: 'Teppo',
      dateOfBirth: LocalDate.of(2019, 1, 1),
      ssn: null
    }).saveChild()
    await Fixture.guardian(child1, guardian).save()
    const application1 = {
      ...applicationFixture(
        child1,
        guardian,
        undefined,
        'PRESCHOOL',
        null,
        [unit1.id],
        false,
        'WAITING_UNIT_CONFIRMATION',
        nextTermStart
      ),
      id: uuidv4()
    }

    const child2 = await Fixture.person({
      lastName: 'Testiläinen',
      firstName: 'Seppo',
      dateOfBirth: LocalDate.of(2019, 1, 2),
      ssn: null
    }).saveChild()
    await Fixture.guardian(child2, guardian).save()
    const application2 = {
      ...applicationFixture(
        child2,
        guardian,
        undefined,
        'PRESCHOOL',
        null,
        [unit2.id],
        false,
        'WAITING_UNIT_CONFIRMATION',
        nextTermStart
      ),
      id: uuidv4()
    }

    await createApplications({ body: [application1, application2] })

    const admin = await Fixture.employee().admin().save()
    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })
    const report = await navigateToReport(page, admin)
    await report.assertRows([
      `${unit1.name}\t${child1.lastName}\t${child1.firstName}\t01.01.2019\t${child1.streetAddress}\t${child1.postalCode}\t\tEi`,
      `${unit2.name}\t${child2.lastName}\t${child2.firstName}\t02.01.2019\t${child2.streetAddress}\t${child2.postalCode}\t\tEi`
    ])
  })
})

const navigateToReport = async (page: Page, user: DevEmployee) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  return await new ReportsPage(page).openPreschoolApplicationReport()
}
