// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import config from '../../config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import {
  DaycarePlacement,
  EmployeeDetail,
  PersonDetailWithDependantsAndGuardians
} from '../../dev-api/types'
import ChildInformationPage, {
  ChildDocumentsSection
} from '../../pages/employee/child-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let admin: EmployeeDetail
let childInformationPage: ChildInformationPage
let child: PersonDetailWithDependantsAndGuardians
let daycarePlacementFixture: DaycarePlacement

beforeAll(async () => {
  await resetDatabase()

  admin = (await Fixture.employeeAdmin().save()).data

  const fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const unitId = fixtures.daycareFixture.id
  child = fixtures.familyWithTwoGuardians.children[0]

  daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    child.id,
    unitId
  )

  await insertDaycarePlacementFixtures([daycarePlacementFixture])

  await Fixture.groupPlacement()
    .with({
      daycareGroupId: daycareGroupFixture.id,
      daycarePlacementId: daycarePlacementFixture.id,
      startDate: daycarePlacementFixture.startDate,
      endDate: daycarePlacementFixture.endDate
    })
    .save()
})

describe('Child Information - Child discussion section', () => {
  let section: ChildDocumentsSection
  beforeEach(async () => {
    page = await Page.open()
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${child.id}`)
    childInformationPage = new ChildInformationPage(page)
    section = await childInformationPage.openCollapsible('childDocuments')
    await section.waitUntilVisible()
  })

  test('Can edit child discussion data', async () => {
    await section.editChildDiscussion()
    await section.offeredDateInput.assertValueEquals('')
    await section.heldDateInput.assertValueEquals('')
    await section.counselingDateInput.assertValueEquals('')
  })

  test('offered date can be edited', async () => {
    const offeredDate = LocalDate.todayInSystemTz().format()

    await section.offeredDate.assertTextEquals('')

    await section.editChildDiscussion()
    await section.offeredDateInput.fill(offeredDate)
    await section.confirmEdited()
    await section.offeredDate.assertTextEquals(offeredDate)

    await section.editChildDiscussion()
    await section.offeredDateInput.fill('')
    await section.confirmEdited()
    await section.offeredDate.assertTextEquals('')
  })

  test('held date can be edited', async () => {
    const heldDate = LocalDate.todayInSystemTz().format()

    await section.heldDate.assertTextEquals('')

    await section.editChildDiscussion()
    await section.heldDateInput.fill(heldDate)
    await section.confirmEdited()
    await section.heldDate.assertTextEquals(heldDate)

    await section.editChildDiscussion()
    await section.heldDateInput.fill('')
    await section.confirmEdited()
    await section.heldDate.assertTextEquals('')
  })

  test('counseling date can be edited', async () => {
    const counselingDate = LocalDate.todayInSystemTz().format()

    await section.counselingDate.assertTextEquals('')

    await section.editChildDiscussion()
    await section.counselingDateInput.fill(counselingDate)
    await section.confirmEdited()
    await section.counselingDate.assertTextEquals(counselingDate)

    await section.editChildDiscussion()
    await section.counselingDateInput.fill('')
    await section.confirmEdited()
    await section.counselingDate.assertTextEquals('')
  })

  test('all fields can be edited', async () => {
    const offeredDate = LocalDate.todayInSystemTz().format()
    const heldDate = LocalDate.todayInSystemTz().addDays(2).format()
    const counselingDate = LocalDate.todayInSystemTz().addDays(5).format()

    await section.offeredDate.assertTextEquals('')
    await section.heldDate.assertTextEquals('')
    await section.counselingDate.assertTextEquals('')

    await section.editChildDiscussion()
    await section.offeredDateInput.fill(offeredDate)
    await section.heldDateInput.fill(heldDate)
    await section.counselingDateInput.fill(counselingDate)
    await section.confirmEdited()
    await section.offeredDate.assertTextEquals(offeredDate)
    await section.heldDate.assertTextEquals(heldDate)
    await section.counselingDate.assertTextEquals(counselingDate)

    await section.editChildDiscussion()
    await section.offeredDateInput.fill('')
    await section.heldDateInput.fill('')
    await section.counselingDateInput.fill('')
    await section.confirmEdited()
    await section.offeredDate.assertTextEquals('')
    await section.heldDate.assertTextEquals('')
    await section.counselingDate.assertTextEquals('')
  })
})
