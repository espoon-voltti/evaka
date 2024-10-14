// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChildDailyNoteBody } from 'lib-common/generated/api-types/note'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
import {
  Fixture,
  testCareArea,
  testChild,
  testDaycare
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import {
  DevDaycare,
  DevDaycareGroup,
  DevPerson
} from '../../generated/api-types'
import ChildAttendancePage from '../../pages/mobile/child-attendance-page'
import MobileChildPage from '../../pages/mobile/child-page'
import MobileListPage from '../../pages/mobile/list-page'
import MobileNotePage from '../../pages/mobile/note-page'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let listPage: MobileListPage
let childPage: MobileChildPage
let notePage: MobileNotePage
let attendancePage: ChildAttendancePage

describe('Child and group notes', () => {
  let child: DevPerson

  beforeEach(async () => {
    await resetServiceState()
    await Fixture.careArea(testCareArea).save()
    await Fixture.daycare(testDaycare).save()
    await Fixture.person(testChild).saveChild()
    child = testChild
    const unit = testDaycare

    const daycareGroup = await Fixture.daycareGroup({
      daycareId: unit.id
    }).save()
    const placementFixture = await Fixture.placement({
      childId: child.id,
      unitId: unit.id
    }).save()
    await Fixture.groupPlacement({
      daycareGroupId: daycareGroup.id,
      daycarePlacementId: placementFixture.id,
      startDate: placementFixture.startDate,
      endDate: placementFixture.endDate
    }).save()

    page = await Page.open()

    const mobileSignupUrl = await pairMobileDevice(unit.id)
    await page.goto(mobileSignupUrl)

    listPage = new MobileListPage(page)
    await listPage.selectChild(child.id)
    childPage = new MobileChildPage(page)
    await childPage.openNotes()

    notePage = new MobileNotePage(page)
    attendancePage = new ChildAttendancePage(page)
  })

  test('Child daily note can be created and deleted', async () => {
    const childDailyNote: ChildDailyNoteBody = {
      note: 'Näki painajaisia',
      feedingNote: 'MEDIUM',
      sleepingMinutes: 65,
      sleepingNote: 'NONE',
      reminders: ['DIAPERS'],
      reminderNote: 'Huomenna metsäretki'
    }

    await notePage.fillNote(childDailyNote)
    await notePage.saveChildDailyNote()
    await childPage.notesExistsBubble.waitUntilVisible()
    await childPage.openNotes()
    await notePage.assertNote(childDailyNote)

    await page.goto(config.mobileUrl)
    await listPage.selectChild(child.id)
    await childPage.markPresentLink.click()
    await attendancePage.dailyNote.assertTextEquals(childDailyNote.note)

    await page.goto(config.mobileUrl)
    await listPage.openChildNotes(child.id)
    await notePage.deleteChildDailyNote()
    await listPage.assertChildNoteDoesntExist(child.id)
  })

  test('Child group note can be created', async () => {
    const groupNote = 'Testiryhmäviesti'

    await notePage.selectTab('group')
    await notePage.typeAndSaveStickyNote(groupNote)
    await notePage.assertStickyNote(groupNote)

    await page.goto(config.mobileUrl)
    await listPage.selectChild(child.id)
    await childPage.markPresentLink.click()
    await attendancePage.groupNotes.assertCount(1)
    await attendancePage.groupNotes.nth(0).assertTextEquals(groupNote)
  })

  test('Sticky notes can be created, edited and deleted', async () => {
    const note = 'tahmea viesti'
    const note2 = 'erittäin tahmea viesti'

    await notePage.selectTab('sticky')

    await notePage.typeAndSaveStickyNote(note)
    await notePage.assertStickyNote(note)

    await notePage.initNewStickyNote()
    await notePage.typeAndSaveStickyNote(note2)

    await notePage.assertStickyNote(note, 0)
    await notePage.assertStickyNoteExpires(
      LocalDate.todayInSystemTz().addDays(7),
      0
    )
    await notePage.assertStickyNote(note2, 1)

    await notePage.editStickyNote('Foobar', 1)
    await notePage.assertStickyNote('Foobar', 1)

    await notePage.removeStickyNote(0)
    await notePage.assertStickyNote('Foobar', 0)

    await page.goto(config.mobileUrl)
    await listPage.selectChild(child.id)
    await childPage.markPresentLink.click()
    await attendancePage.stickyNotes.assertCount(1)
    await attendancePage.stickyNotes.nth(0).assertTextEquals('Foobar')
  })
})

describe('Child and group notes (backup care)', () => {
  let child: DevPerson
  let backupCareDaycareGroup: DevDaycareGroup
  let backupCareDaycare: DevDaycare

  beforeEach(async () => {
    await resetServiceState()
    await Fixture.careArea(testCareArea).save()
    await Fixture.daycare(testDaycare).save()
    await Fixture.person(testChild).saveChild()
    child = testChild
    const unit = testDaycare

    const daycareGroup = await Fixture.daycareGroup({
      daycareId: unit.id
    }).save()
    const placementFixture = await Fixture.placement({
      childId: child.id,
      unitId: unit.id
    }).save()
    await Fixture.groupPlacement({
      daycareGroupId: daycareGroup.id,
      daycarePlacementId: placementFixture.id,
      startDate: placementFixture.startDate,
      endDate: placementFixture.endDate
    }).save()

    const careArea = await Fixture.careArea().save()
    backupCareDaycare = await Fixture.daycare({ areaId: careArea.id }).save()
    backupCareDaycareGroup = await Fixture.daycareGroup({
      daycareId: backupCareDaycare.id
    }).save()
    await Fixture.backupCare({
      unitId: backupCareDaycareGroup.daycareId,
      groupId: backupCareDaycareGroup.id,
      childId: child.id
    }).save()

    page = await Page.open()

    const mobileSignupUrl = await pairMobileDevice(backupCareDaycare.id)
    await page.goto(mobileSignupUrl)

    listPage = new MobileListPage(page)
    await listPage.selectChild(child.id)
    childPage = new MobileChildPage(page)
    await childPage.openNotes()

    notePage = new MobileNotePage(page)
  })

  test('Child group note can be created', async () => {
    const groupNote = 'Testiryhmäviesti'

    await notePage.selectTab('group')
    await notePage.typeAndSaveStickyNote(groupNote)
    await notePage.assertStickyNote(groupNote)
  })
})
