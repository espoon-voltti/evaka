// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import config from '../../config'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import { daycareGroupFixture, Fixture } from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDefaultServiceNeedOptions,
  resetDatabase
} from '../../generated/api-clients'
import { AssistanceNeedsAndActionsReport } from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let childId: UUID
let unitId: UUID

const mockedTime = LocalDate.of(2024, 2, 19)

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await createDefaultServiceNeedOptions()
  await createDaycareGroups({ body: [daycareGroupFixture] })

  unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id
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
      daycareGroupId: daycareGroupFixture.id,
      daycarePlacementId: placementBuilder.data.id,
      startDate: mockedTime,
      endDate: mockedTime
    })
    .save()

  page = await Page.open({
    mockedTime: mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  await employeeLogin(page, (await Fixture.employeeAdmin().save()).data)
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
    await report.assertUnitRow(0, 'Superkeskus,,,1,0,0,0,1,0,0,1,0,0,1')
    await report.selectCareAreaFilter('Superkeskus')
    await report.openUnit('Alkuräjähdyksen päiväkoti')
    await report.assertChildRow(
      0,
      'Antero,Onni,Leevi,Aatu,Högfors,Kosmiset,Vakiot,10,1,0,0,0,1,0,0,a,test,assistance,action,option,1.5'
    )
  })
})
