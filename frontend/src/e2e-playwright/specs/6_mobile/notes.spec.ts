// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
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
import { newBrowserContext } from '../../browser'
import MobileListPage from '../../pages/mobile/list-page'
import MobileChildPage from '../../pages/mobile/child-page'
import { pairMobileDevice } from '../../utils/mobile'
import {
  DaycarePlacement,
  PersonDetail
} from '../../../e2e-test-common/dev-api/types'
import MobileNotePage from '../../pages/mobile/note-page'
import {
  ChildDailyNoteBody,
  GroupNoteBody
} from 'lib-common/generated/api-types/note'
import LocalDate from 'lib-common/local-date'

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

  page = await (await newBrowserContext()).newPage()

  const mobileSignupUrl = await pairMobileDevice(
    employee.data.id!, // eslint-disable-line
    unit.id
  )
  await page.goto(mobileSignupUrl)

  await new MobileListPage(page).selectChild(child.id)
  childPage = new MobileChildPage(page)
  await childPage.openNotes()

  notePage = new MobileNotePage(page)
})
afterEach(async () => {
  await page.close()
})

describe('Child and group notes', () => {
  test('Child daily note can be created', async () => {
    const daycareDailyNote: ChildDailyNoteBody = {
      note: 'Testiviesti',
      feedingNote: 'MEDIUM',
      sleepingMinutes: 65,
      sleepingNote: 'NONE',
      reminders: ['DIAPERS'],
      reminderNote: 'Näki painajaisia'
    }

    await notePage.fillNote(daycareDailyNote)
    await notePage.saveNote()
    await childPage.assertNotesExist()
    await childPage.openNotes()
    await notePage.assertNote(daycareDailyNote)
  })

  test('Child group note can be created', async () => {
    const groupNote: GroupNoteBody = {
      note: 'Testiryhmäviesti',
      expires: LocalDate.today().addDays(7)
    }

    await notePage.selectGroupTab()
    await notePage.fillGroupNote(groupNote)
    await notePage.saveNote()
    await childPage.assertNotesExist()
    await childPage.openNotes()
    await notePage.selectGroupTab()
    await notePage.assertGroupNote(groupNote)
  })
})
