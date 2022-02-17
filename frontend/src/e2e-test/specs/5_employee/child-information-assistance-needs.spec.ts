// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { addDays, subDays } from 'date-fns'

import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertDefaultServiceNeedOptions,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { EmployeeDetail } from '../../dev-api/types'
import ChildInformationPage, {
  AssistanceNeedSection
} from '../../pages/employee/child-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let childInformationPage: ChildInformationPage
let assistanceNeeds: AssistanceNeedSection
let childId: UUID
let unitId: UUID

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id
  page = await Page.open()
})

const setupPlacement = async (childPlacementType: PlacementType) => {
  await insertDaycarePlacementFixtures([
    createDaycarePlacementFixture(
      uuidv4(),
      childId,
      unitId,
      LocalDate.today().formatIso(),
      LocalDate.today().formatIso(),
      childPlacementType
    )
  ])
}

const logUserIn = async (user: EmployeeDetail) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
  assistanceNeeds = await childInformationPage.openCollapsible('assistanceNeed')
}

describe('Child Information assistance need functionality for employees', () => {
  test('assistance need can be added', async () => {
    await setupPlacement('DAYCARE')
    await logUserIn((await Fixture.employeeUnitSupervisor(unitId).save()).data)
    await assistanceNeeds.createNewAssistanceNeed()
    await assistanceNeeds.setAssistanceNeedMultiplier('1,5')
    await assistanceNeeds.confirmAssistanceNeed()
    await assistanceNeeds.assertAssistanceNeedMultiplier('1,5')
  })

  test('assistance need before preschool for a child in preschool is not shown for unit supervisor', async () => {
    await setupPlacement('PRESCHOOL')
    const unitSupervisor = (await Fixture.employeeUnitSupervisor(unitId).save())
      .data
    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: subDays(new Date(), 1),
        endDate: new Date(),
        description:
          'Test service need to be hidden because it starts before preschool started',
        updatedBy: unitSupervisor.id
      })
      .save()

    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: addDays(new Date(), 1),
        endDate: addDays(new Date(), 1),
        description:
          'Test service need to be shown because it starts after preschool started',
        updatedBy: unitSupervisor.id
      })
      .save()

    await logUserIn(unitSupervisor)
    await assistanceNeeds.assertAssistanceNeedCount(1)
  })

  test('assistance need for preschool for a child in preschool is shown for unit manager', async () => {
    await setupPlacement('PRESCHOOL')
    const unitSupervisor = (await Fixture.employeeUnitSupervisor(unitId).save())
      .data
    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: new Date(),
        description:
          'Test service need to be shown because it starts when preschool started',
        updatedBy: unitSupervisor.id
      })
      .save()

    await logUserIn(unitSupervisor)
    await assistanceNeeds.assertAssistanceNeedCount(1)
  })

  test('assistance need before preschool for a child in preschool is shown for admin', async () => {
    await setupPlacement('PRESCHOOL')
    const admin = (await Fixture.employeeAdmin().save()).data
    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: subDays(new Date(), 1),
        description: 'Test service need to be shown because user is admin',
        updatedBy: admin.id
      })
      .save()

    await logUserIn(admin)
    await assistanceNeeds.assertAssistanceNeedCount(1)
  })

  test('assistance need before preschool for a child in preschool is shown for Special Education Teacher', async () => {
    await setupPlacement('PRESCHOOL')
    const specialEducationTeacher = (
      await Fixture.employeeSpecialEducationTeacher(unitId).save()
    ).data

    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: subDays(new Date(), 1),
        endDate: new Date(),
        description:
          'Test service need to be shown to SEO if she has acl rights to child',
        updatedBy: specialEducationTeacher.id
      })
      .save()

    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: addDays(new Date(), 1),
        endDate: addDays(new Date(), 2),
        description:
          'Test service to be shown because it is active only during preschool period',
        updatedBy: specialEducationTeacher.id
      })
      .save()

    await logUserIn(specialEducationTeacher)
    await assistanceNeeds.assertAssistanceNeedCount(2)
  })
})
