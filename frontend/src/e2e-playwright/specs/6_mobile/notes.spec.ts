// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChildDailyNoteBody } from 'lib-common/generated/api-types/note'
import LocalDate from 'lib-common/local-date'
import {
  insertDaycareGroupPlacementFixtures,
  insertDaycarePlacementFixtures,
  resetDatabase,
  setAclForDaycares
} from '../../../e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../../e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  DaycareGroupBuilder,
  EmployeeBuilder,
  Fixture,
  uuidv4
} from '../../../e2e-test-common/dev-api/fixtures'
import {
  DaycarePlacement,
  PersonDetail
} from '../../../e2e-test-common/dev-api/types'
import MobileChildPage from '../../pages/mobile/child-page'
import MobileListPage from '../../pages/mobile/list-page'
import MobileNotePage from '../../pages/mobile/note-page'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let childPage: MobileChildPage
let notePage: MobileNotePage

let fixtures: AreaAndPersonFixtures
let child: PersonDetail
let employee: EmployeeBuilder
const employeeId = uuidv4()

const daycareGroupPlacementId = uuidv4()

let daycarePlacementFixture: DaycarePlacement
let daycareGroup: DaycareGroupBuilder

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  child = fixtures.enduserChildFixtureJari
  const unit = fixtures.daycareFixture

  employee = await Fixture.employee()
    .with({
      id: employeeId,
      externalId: `espooad: ${employeeId}`,
      firstName: 'Yrjö',
      lastName: 'Yksikkö',
      email: 'yy@example.com',
      roles: []
    })
    .save()

  daycareGroup = await Fixture.daycareGroup()
    .with({ daycareId: unit.id })
    .save()
  daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    child.id,
    unit.id
  )
  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  await setAclForDaycares(employee.data.externalId!, unit.id)
  await insertDaycarePlacementFixtures([daycarePlacementFixture])
  await insertDaycareGroupPlacementFixtures([
    {
      id: daycareGroupPlacementId,
      daycareGroupId: daycareGroup.data.id,
      daycarePlacementId: daycarePlacementFixture.id,
      startDate: daycarePlacementFixture.startDate,
      endDate: daycarePlacementFixture.endDate
    }
  ])

  page = await Page.open()

  const mobileSignupUrl = await pairMobileDevice(unit.id)
  await page.goto(mobileSignupUrl)

  await new MobileListPage(page).selectChild(child.id)
  childPage = new MobileChildPage(page)
  await childPage.openNotes()

  notePage = new MobileNotePage(page)
})

describe('Child and group notes', () => {
  test('Child daily note can be created', async () => {
    const childDailyNote: ChildDailyNoteBody = {
      note: 'Testiviesti',
      feedingNote: 'MEDIUM',
      sleepingMinutes: 65,
      sleepingNote: 'NONE',
      reminders: ['DIAPERS'],
      reminderNote: 'Näki painajaisia'
    }

    await notePage.fillNote(childDailyNote)
    await notePage.saveChildDailyNote()
    await childPage.assertNotesExist()
    await childPage.openNotes()
    await notePage.assertNote(childDailyNote)
  })

  test('Child group note can be created', async () => {
    const groupNote = 'Testiryhmäviesti'

    await notePage.selectTab('group')
    await notePage.typeAndSaveStickyNote(groupNote)
    await notePage.assertStickyNote(groupNote)
  })

  test('Sticky notes can be created, edited and removed', async () => {
    const note = 'tahmea viesti'
    const note2 = 'erittäin tahmea viesti'

    await notePage.selectTab('sticky')

    await notePage.typeAndSaveStickyNote(note)
    await notePage.assertStickyNote(note)

    await notePage.initNewStickyNote()
    await notePage.typeAndSaveStickyNote(note2)

    await notePage.assertStickyNote(note, 0)
    await notePage.assertStickyNoteExpires(LocalDate.today().addDays(7), 0)
    await notePage.assertStickyNote(note2, 1)

    await notePage.editStickyNote('Foobar', 1)
    await notePage.assertStickyNote('Foobar', 1)

    await notePage.removeStickyNote(0)
    await notePage.assertStickyNote('Foobar', 0)
  })
})
