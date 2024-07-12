// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  testDaycareGroup,
  Fixture,
  familyWithTwoGuardians,
  testCareArea,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import { AssistanceNeedsAndActionsReport } from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let childId: UUID
let unitId: UUID
let admin: DevEmployee

const mockedTime = LocalDate.of(2024, 2, 19)

beforeEach(async () => {
  await resetServiceState()

  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare().with(testDaycare).save()
  await Fixture.family(familyWithTwoGuardians).save()
  await createDefaultServiceNeedOptions()
  await createDaycareGroups({ body: [testDaycareGroup] })

  unitId = testDaycare.id
  childId = familyWithTwoGuardians.children[0].id
  const placementBuilder = await Fixture.placement()
    .with({
      childId,
      unitId: unitId,
      startDate: mockedTime,
      endDate: mockedTime
    })
    .save()

  await Fixture.groupPlacement()
    .with({
      daycareGroupId: testDaycareGroup.id,
      daycarePlacementId: placementBuilder.id,
      startDate: mockedTime,
      endDate: mockedTime
    })
    .save()

  page = await Page.open({
    mockedTime: mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  admin = await Fixture.employeeAdmin().save()
  await employeeLogin(page, admin)
})

describe('Assistance need and actions report', () => {
  test('Shows assistance needs', async () => {
    const validDuring = new FiniteDateRange(mockedTime, mockedTime)

    await Fixture.daycareAssistance()
      .with({
        childId,
        validDuring
      })
      .save()

    await Fixture.otherAssistanceMeasure()
      .with({
        childId,
        validDuring
      })
      .save()

    await Fixture.assistanceActionOption()
      .with({
        value: 'ASSISTANCE_SERVICE_CHILD'
      })
      .save()

    await Fixture.assistanceAction()
      .with({
        childId,
        updatedBy: admin.id,
        startDate: validDuring.start,
        endDate: validDuring.end,
        actions: ['ASSISTANCE_SERVICE_CHILD']
      })
      .save()

    await Fixture.assistanceNeedVoucherCoefficient()
      .with({
        childId,
        validityPeriod: new FiniteDateRange(validDuring.start, validDuring.end),
        coefficient: 1.5
      })
      .save()

    await page.goto(
      `${config.employeeUrl}/reports/assistance-needs-and-actions`
    )
    const report = new AssistanceNeedsAndActionsReport(page)
    await report.needsAndActionsRows
      .nth(0)
      .assertTextEquals('Superkeskus\n' + '\t\t1\t0\t0\t0\t1\t0\t0\t1\t0\t0\t1')
    await report.selectCareAreaFilter('Superkeskus')
    await report.openUnit('Alkuräjähdyksen päiväkoti')
    await report.childRows
      .nth(0)
      .assertTextEquals(
        'Antero Onni Leevi Aatu Högfors\tKosmiset Vakiot\t10\t1\t0\t0\t0\t1\t0\t0\ta test assistance action option\t1.5'
      )
  })
})
