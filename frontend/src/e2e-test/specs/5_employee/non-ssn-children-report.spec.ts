// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevEmployee, DevPerson } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import type { NonSsnChildrenReport } from '../../pages/employee/reports'
import ReportsPage from '../../pages/employee/reports'
import { test } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedToday = LocalDate.of(2023, 6, 12)

test.describe('Non SSN children report', () => {
  test.use({
    evakaOptions: {
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    }
  })

  let child: DevPerson
  let child2: DevPerson
  let child3: DevPerson
  let child4: DevPerson

  test.beforeEach(async () => {
    await resetServiceState()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare({ areaId: area.id }).save()
    child = await Fixture.person({
      firstName: 'Esko',
      lastName: 'Beck',
      ssn: null,
      ophPersonOid: 'mock-oid-1'
    }).saveChild()
    child2 = await Fixture.person({
      firstName: 'Maija',
      lastName: 'Äänikolu',
      ssn: null
    }).saveChild()
    child3 = await Fixture.person({
      firstName: 'Pasi',
      lastName: 'Pastplacement',
      ssn: null
    }).saveChild()
    child4 = await Fixture.person({
      firstName: 'Sami',
      lastName: 'Ssnhaver',
      ssn: '050520A999M'
    }).saveChild()

    await Fixture.placement({
      type: 'DAYCARE',
      childId: child.id,
      unitId: unit.id,
      startDate: mockedToday,
      endDate: mockedToday.addDays(4)
    }).save()
    await Fixture.placement({
      type: 'DAYCARE',
      childId: child2.id,
      unitId: unit.id,
      startDate: mockedToday.addDays(7),
      endDate: mockedToday.addDays(14)
    }).save()

    await Fixture.placement({
      type: 'DAYCARE',
      childId: child3.id,
      unitId: unit.id,
      startDate: mockedToday.subMonths(6),
      endDate: mockedToday.subMonths(3)
    }).save()

    await Fixture.placement({
      type: 'DAYCARE',
      childId: child4.id,
      unitId: unit.id,
      startDate: mockedToday.subMonths(1),
      endDate: mockedToday.addMonths(1)
    }).save()
  })

  test('report data is shown to admin', async ({ evaka }) => {
    const admin = await Fixture.employee().admin().save()

    const report = await navigateToReport(evaka, admin)
    await assertReport(report)
  })

  // This test is skipped until e2e tests use default action rules instead of Espoo customizations
  test.skip('report data is shown to finance admin', async ({ evaka }) => {
    const financeAdmin = await Fixture.employee().financeAdmin().save()

    const report = await navigateToReport(evaka, financeAdmin)
    await assertReport(report)
  })

  test('report data is shown to service worker', async ({ evaka }) => {
    const serviceWorker = await Fixture.employee().serviceWorker().save()

    await employeeLogin(evaka, serviceWorker)
    const report = await navigateToReport(evaka, serviceWorker)
    await assertReport(report)
  })

  const assertReport = async (report: NonSsnChildrenReport) => {
    const initialExpectation = [
      {
        childName: `${child.lastName} ${child.firstName}`,
        dateOfBirth: child.dateOfBirth.format(),
        ophPersonOid: child.ophPersonOid ?? '',
        lastSentToVarda: '-'
      },
      {
        childName: `${child2.lastName} ${child2.firstName}`,
        dateOfBirth: child2.dateOfBirth.format(),
        ophPersonOid: child2.ophPersonOid ?? '',
        lastSentToVarda: '-'
      }
    ]

    await report.assertRows(initialExpectation)

    await report.changeSortOrder()

    await report.assertRows(initialExpectation.reverse())
  }

  const navigateToReport = async (page: Page, user: DevEmployee) => {
    await employeeLogin(page, user)
    await page.goto(config.employeeUrl)
    await new EmployeeNav(page).openTab('reports')
    return await new ReportsPage(page).openNonSsnChildrenReport()
  }
})
