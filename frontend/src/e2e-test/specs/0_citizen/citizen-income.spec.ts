// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  EmployeeId,
  ServiceNeedId
} from 'lib-common/generated/api-types/shared'
import { evakaUserId, randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import {
  testCareArea,
  testDaycare,
  testDaycareGroup,
  testChild,
  testAdult,
  Fixture
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevDaycare, DevPerson } from '../../generated/api-types'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import CitizenHeader from '../../pages/citizen/citizen-header'
import IncomeStatementsPage from '../../pages/citizen/citizen-income'
import { waitUntilEqual } from '../../utils'
import { envs, Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

describe.each(envs)('Citizen income (%s)', (env) => {
  let page: Page
  const child = testChild
  let daycare: DevDaycare
  let guardian: DevPerson
  let financeAdminId: EmployeeId

  const today = LocalDate.of(2022, 1, 5)
  const placementStart = today
  const placementEnd = placementStart.addYears(1)

  beforeEach(async () => {
    await resetServiceState()

    const area = await testCareArea.save()
    daycare = await Fixture.daycare({
      ...testDaycare,
      areaId: area.id,
      openingDate: placementStart.subYears(1)
    }).save()
    await Fixture.daycareGroup({
      ...testDaycareGroup,
      daycareId: daycare.id
    }).save()

    const child1 = await child.saveChild({
      updateMockVtj: true
    })
    guardian = await testAdult.saveAdult({
      updateMockVtjWithDependants: [child]
    })
    await Fixture.guardian(child1, guardian).save()
    const placement = await Fixture.placement({
      childId: child1.id,
      unitId: daycare.id,
      startDate: placementStart,
      endDate: placementEnd
    }).save()

    const daycareGroup = await Fixture.daycareGroup({
      daycareId: daycare.id,
      name: 'Group 1'
    }).save()

    await Fixture.groupPlacement({
      startDate: placementStart,
      endDate: placementEnd,
      daycareGroupId: daycareGroup.id,
      daycarePlacementId: placement.id
    }).save()

    const financeAdmin = await Fixture.employee().financeAdmin().save()
    financeAdminId = financeAdmin.id

    const serviceNeedOption = await Fixture.serviceNeedOption({
      feeCoefficient: 42.0
    }).save()

    await Fixture.serviceNeed({
      id: randomId<ServiceNeedId>(),
      placementId: placement.id,
      startDate: placementStart,
      endDate: placementEnd,
      optionId: serviceNeedOption.id,
      shiftCare: 'NONE',
      confirmedBy: evakaUserId(financeAdmin.id),
      confirmedAt: placementStart.toHelsinkiDateTime(LocalTime.of(12, 0))
    }).save()

    const viewport =
      env === 'mobile'
        ? { width: 375, height: 812 }
        : { width: 1920, height: 1080 }

    page = await Page.open({
      viewport,
      screen: viewport,
      mockedTime: today.toHelsinkiDateTime(LocalTime.of(12, 0))
    })
  })

  test('Citizen sees expiring income cta and it does not appear again if income statement is done', async () => {
    const incomeEndDate = today.addWeeks(4).subDays(1)
    await Fixture.income({
      personId: guardian.id,
      validFrom: placementStart,
      validTo: incomeEndDate,
      modifiedBy: evakaUserId(financeAdminId),
      modifiedAt: placementStart.toHelsinkiDateTime(LocalTime.of(0, 0))
    }).save()

    await Fixture.fridgeChild({
      childId: child.id,
      headOfChild: guardian.id,
      startDate: placementStart,
      endDate: placementEnd
    }).save()

    await enduserLogin(page, guardian)
    const header = new CitizenHeader(page, env)
    await header.selectTab('calendar')
    const calendar = new CitizenCalendarPage(page, 'desktop')
    await waitUntilEqual(
      () => calendar.getExpiringIncomeCtaContent(),
      'Muista päivittää tulotietosi 01.02.2022 mennessä'
    )
    await calendar.clickExpiringIncomeCta()

    const incomeStatementsPage = new IncomeStatementsPage(page, env)
    await incomeStatementsPage.createNewIncomeStatement()
    await incomeStatementsPage.selectIncomeStatementType('highest-fee')
    await incomeStatementsPage.setValidFromDate(today.format())
    await incomeStatementsPage.checkAssured()
    await incomeStatementsPage.submit()

    await waitUntilEqual(async () => await incomeStatementsPage.rows.count(), 1)
    await incomeStatementsPage.rows
      .only()
      .assertText((text) => text.includes(today.format()))

    await header.selectTab('calendar')
    await calendar.assertExpiringIncomeCtaNotVisible()
  })
})
