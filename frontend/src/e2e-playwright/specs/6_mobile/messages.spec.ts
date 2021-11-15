// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { newBrowserContext } from 'e2e-playwright/browser'
import MobileChildPage from 'e2e-playwright/pages/mobile/child-page'
import MobileListPage from 'e2e-playwright/pages/mobile/list-page'
import PinLoginPage from 'e2e-playwright/pages/mobile/pin-login-page'
import { pairMobileDevice } from 'e2e-playwright/utils/mobile'
import config from 'e2e-test-common/config'
import {
  insertDaycareGroupPlacementFixtures,
  insertDaycarePlacementFixtures,
  insertParentshipFixtures,
  resetDatabase,
  setAclForDaycares,
  upsertMessageAccounts
} from 'e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  DaycareGroupBuilder,
  EmployeeBuilder,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { DaycarePlacement, PersonDetail } from 'e2e-test-common/dev-api/types'
import { Page } from 'playwright'
import MobileMessageEditorPage from '../../pages/mobile/message-editor'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import { enduserLogin } from '../../utils/user'
import MobileMessagesPage from '../../pages/mobile/messages'
import ThreadViewPage from '../../pages/mobile/thread-view'
import { waitUntilEqual } from '../../utils'

let page: Page
let fixtures: AreaAndPersonFixtures
let listPage: MobileListPage
let childPage: MobileChildPage
let citizenPage: Page
let messageEditorPage: MobileMessageEditorPage
let messagesPage: MobileMessagesPage
let threadView: ThreadViewPage
let pinLoginPage: PinLoginPage

const employeeId = uuidv4()
const daycareGroupPlacementId = uuidv4()

let daycarePlacementFixture: DaycarePlacement
let daycareGroup: DaycareGroupBuilder
let employee: EmployeeBuilder
let child: PersonDetail

const empFirstName = 'Yrjö'
const empLastName = 'Yksikkö'
const employeeName = `${empLastName} ${empFirstName}`

const pin = '2580'

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  child = fixtures.enduserChildFixtureJari
  const unit = fixtures.daycareFixture

  employee = await Fixture.employee()
    .with({
      id: employeeId,
      externalId: `espooad: ${employeeId}`,
      firstName: empFirstName,
      lastName: empLastName,
      email: 'yy@example.com',
      roles: []
    })
    .save()

  await Fixture.employeePin().with({ userId: employee.data.id, pin }).save()
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
  await upsertMessageAccounts()
  await insertParentshipFixtures([
    {
      childId: child.id,
      headOfChildId: fixtures.enduserGuardianFixture.id,
      startDate: '2021-01-01',
      endDate: '2028-01-01'
    }
  ])

  page = await (await newBrowserContext()).newPage()
  listPage = new MobileListPage(page)
  childPage = new MobileChildPage(page)
  pinLoginPage = new PinLoginPage(page)
  messageEditorPage = new MobileMessageEditorPage(page)
  messagesPage = new MobileMessagesPage(page)
  threadView = new ThreadViewPage(page)

  const mobileSignupUrl = await pairMobileDevice(
    employee.data.id!, // eslint-disable-line
    unit.id
  )
  await page.goto(mobileSignupUrl)
})
afterEach(async () => {
  await page.close()
})

async function initCitizenPage() {
  citizenPage = await (await newBrowserContext()).newPage()
  await enduserLogin(citizenPage)
}

describe('Message editor in child page', () => {
  test('User can open editor and send message', async () => {
    await listPage.selectChild(child.id)
    await childPage.openMessageEditor()
    await pinLoginPage.login(employeeName, pin)
    await messageEditorPage.draftNewMessage('Foo', 'Bar')
    await messageEditorPage.sendEditedMessage()
  })
})

describe('Child message thread', () => {
  beforeEach(async () => {
    await Promise.all([initCitizenPage()])
  })
  test('User can reply to thread', async () => {
    const title = 'Otsikko'
    const content = 'Testiviestin sisältö'
    const receivers = [`${empLastName} ${empFirstName}`]
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    await citizenMessagesPage.sendNewMessage(title, content, receivers)
    await citizenPage.close()

    await listPage.gotoMessages()
    await pinLoginPage.login(employeeName, pin)
    await messagesPage.messagesExist()
    await messagesPage.openThread()
    await waitUntilEqual(() => threadView.countMessages(), 1)
    await waitUntilEqual(() => threadView.getMessageContent(1), content)

    const replyContent = 'Testivastauksen sisältö'
    await threadView.replyThread(replyContent)
    await waitUntilEqual(() => threadView.countMessages(), 2)
    await waitUntilEqual(() => threadView.getMessageContent(2), replyContent)
  })
})
