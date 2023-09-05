// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import config from '../../config'
import {
  getChildDiscussionsByChildId,
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
import { waitUntilEqual } from '../../utils'
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
    page = await Page.open({
      mockedTime: LocalDate.of(2023, 7, 1).toSystemTzDate(),
      overrides: {
        featureFlags: { childDiscussion: true }
      }
    })
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${child.id}`)
    childInformationPage = new ChildInformationPage(page)
    section = await childInformationPage.openCollapsible('childDocuments')
  })

  test('Can add child discussion data', async () => {
    await section.addChildDiscussion()

    await section.addOfferedDateInput.assertValueEquals('')
    await section.addHeldDateInput.assertValueEquals('')
    await section.addCounselingDateInput.assertValueEquals('')

    const offeredDate = LocalDate.of(2023, 7, 1)
    const heldDate = LocalDate.of(2023, 7, 3)
    const counselingDate = LocalDate.of(2023, 7, 5)

    await section.addOfferedDateInput.fill(offeredDate.format())
    await section.addHeldDateInput.fill(heldDate.format())
    await section.addCounselingDateInput.fill(counselingDate.format())
    await section.addCounselingDateInput.press('Enter')

    await section.modalOk.click()
    await section.modalOk.waitUntilHidden()

    const savedDiscussions = await getChildDiscussionsByChildId(child.id)
    const newDiscussion = savedDiscussions[0]

    await section
      .offeredDateText(newDiscussion.id)
      .assertTextEquals(offeredDate.format())
    await section
      .heldDateText(newDiscussion.id)
      .assertTextEquals(heldDate.format())
    await section
      .counselingDateText(newDiscussion.id)
      .assertTextEquals(counselingDate.format())
  })
  test('Can edit child discussion data', async () => {
    /* Add discussion entry for editing */
    await section.addChildDiscussion()

    const offeredDate = LocalDate.of(2023, 7, 1)
    const heldDate = LocalDate.of(2023, 7, 3)
    const counselingDate = LocalDate.of(2023, 7, 5)

    await section.addOfferedDateInput.fill(offeredDate.format())
    await section.addHeldDateInput.fill(heldDate.format())
    await section.addCounselingDateInput.fill(counselingDate.format())
    await section.addCounselingDateInput.press('Enter')

    await section.modalOk.click()
    await section.modalOk.waitUntilHidden()

    const savedDiscussions = await getChildDiscussionsByChildId(child.id)
    const { id: discussionId } = savedDiscussions[0]

    await section
      .offeredDateText(discussionId)
      .assertTextEquals(offeredDate.format())
    await section.heldDateText(discussionId).assertTextEquals(heldDate.format())
    await section
      .counselingDateText(discussionId)
      .assertTextEquals(counselingDate.format())

    /* Edit discussion entry */
    await section.editChildDiscussion(discussionId)

    const newOfferedDate = LocalDate.of(2023, 8, 4)
    const newHeldDate = LocalDate.of(2023, 8, 6)
    const newCounselingDate = LocalDate.of(2023, 8, 8)

    await section.offeredDateInput(discussionId).fill(newOfferedDate.format())
    await section.heldDateInput(discussionId).fill(newHeldDate.format())
    await section
      .counselingDateInput(discussionId)
      .fill(newCounselingDate.format())
    await section.counselingDateInput(discussionId).press('Enter')

    await section.confirmEdited(discussionId)

    await section
      .offeredDateText(discussionId)
      .assertTextEquals(newOfferedDate.format())
    await section
      .heldDateText(discussionId)
      .assertTextEquals(newHeldDate.format())
    await section
      .counselingDateText(discussionId)
      .assertTextEquals(newCounselingDate.format())
  })

  test('Can delete child discussion entry', async () => {
    /* Add discussion entry for deletion */
    await section.addChildDiscussion()

    const offeredDate = LocalDate.of(2023, 7, 1)
    const heldDate = LocalDate.of(2023, 7, 3)
    const counselingDate = LocalDate.of(2023, 7, 5)

    await section.addOfferedDateInput.fill(offeredDate.format())
    await section.addHeldDateInput.fill(heldDate.format())
    await section.addCounselingDateInput.fill(counselingDate.format())
    await section.addCounselingDateInput.press('Enter')

    await section.modalOk.click()
    await section.modalOk.waitUntilHidden()

    const savedDiscussions = await getChildDiscussionsByChildId(child.id)
    const { id: discussionId } = savedDiscussions[0]

    await section
      .offeredDateText(discussionId)
      .assertTextEquals(offeredDate.format())
    await section.heldDateText(discussionId).assertTextEquals(heldDate.format())
    await section
      .counselingDateText(discussionId)
      .assertTextEquals(counselingDate.format())

    /* Delete discussion entry */
    await section.deleteChildDiscussion(discussionId)

    await waitUntilEqual(
      () => section.offeredDateText(discussionId).visible,
      false
    )
    await waitUntilEqual(
      () => section.heldDateText(discussionId).visible,
      false
    )
    await waitUntilEqual(
      () => section.counselingDateText(discussionId).visible,
      false
    )
  })
})
