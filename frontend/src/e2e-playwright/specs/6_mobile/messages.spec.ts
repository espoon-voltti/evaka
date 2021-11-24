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
  setAclForDaycareGroups,
  upsertMessageAccounts
} from 'e2e-test-common/dev-api'
import { Daycare } from 'e2e-test-common/dev-api/types'
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
import MobileNav from '../../pages/mobile/mobile-nav'

let page: Page
let fixtures: AreaAndPersonFixtures
let listPage: MobileListPage
let childPage: MobileChildPage
let citizenPage: Page
let messageEditorPage: MobileMessageEditorPage
let messagesPage: MobileMessagesPage
let threadView: ThreadViewPage
let pinLoginPage: PinLoginPage
let nav: MobileNav

const employeeId = uuidv4()
const staffId = uuidv4()
const daycareGroupPlacementId = uuidv4()

let daycarePlacementFixture: DaycarePlacement
let daycareGroup: DaycareGroupBuilder
let employee: EmployeeBuilder
let staff: EmployeeBuilder
let child: PersonDetail

let unit: Daycare

const empFirstName = 'Yrjö'
const empLastName = 'Yksikkö'
const employeeName = `${empLastName} ${empFirstName}`

const staffFirstName = 'Sari'
const staffLastName = 'Staff'
const staffName = `${staffLastName} ${staffFirstName}`

const pin = '2580'

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  child = fixtures.enduserChildFixtureJari
  unit = fixtures.daycareFixture

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

  staff = await Fixture.employee()
    .with({
      id: staffId,
      externalId: `espooad: ${staffId}`,
      firstName: staffFirstName,
      lastName: staffLastName,
      email: 'zz@example.com',
      roles: []
    })
    .save()

  await Fixture.employeePin().with({ userId: employee.data.id, pin }).save()
  await Fixture.employeePin().with({ userId: staff.data.id, pin }).save()

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
  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  await setAclForDaycares(staff.data.externalId!, unit.id, 'STAFF')
  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  await setAclForDaycareGroups(staff.data.id!, unit.id, [daycareGroup.data.id])

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
  nav = new MobileNav(page)

  const mobileSignupUrl = await pairMobileDevice(
    employee.data.id!, // eslint-disable-line
    unit.id
  )
  await page.goto(mobileSignupUrl)

  const staffMobileSignupUrl = await pairMobileDevice(
    staff.data.id!, // eslint-disable-line
    unit.id
  )
  await page.goto(staffMobileSignupUrl)
})
afterEach(async () => {
  await page.close()
})

async function initCitizenPage() {
  citizenPage = await (await newBrowserContext()).newPage()
  await enduserLogin(citizenPage)
}

describe('Message editor in child page', () => {
  test('Employee can open editor and send message', async () => {
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

  test('Supervisor can reply to thread', async () => {
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

  test("Staff sees citizen's message for group", async () => {
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    const title = 'Otsikko'
    const content = 'Testiviestin sisältö'
    const receivers = [daycareGroup.data.name + ' (Henkilökunta)']
    await citizenMessagesPage.sendNewMessage(title, content, receivers)
    await citizenPage.close()

    await listPage.gotoMessages()
    await pinLoginPage.login(staffName, pin)
    await nav.selectGroup(daycareGroup.data.id)
    await messagesPage.messagesExist()
    await messagesPage.openThread()
    await waitUntilEqual(() => threadView.countMessages(), 1)
    await waitUntilEqual(() => threadView.getMessageContent(1), content)
  })

  test('Supervisor replies as a group to message sent to group', async () => {
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    const title = 'Otsikko'
    const content = 'Testiviestin sisältö'
    const receivers = [daycareGroup.data.name + ' (Henkilökunta)']
    await citizenMessagesPage.sendNewMessage(title, content, receivers)
    await citizenPage.close()

    await listPage.gotoMessages()
    await pinLoginPage.login(employeeName, pin)
    await nav.selectGroup(daycareGroup.data.id)
    await messagesPage.messagesExist()
    await messagesPage.openThread()
    const replyContent = 'Testivastauksen sisältö'
    await threadView.replyThread(replyContent)
    await waitUntilEqual(() => threadView.countMessages(), 2)
    await waitUntilEqual(() => threadView.getMessageContent(2), replyContent)
    await waitUntilEqual(
      () => threadView.getMessageSender(2),
      `${unit.name} - ${daycareGroup.data.name}`
    )
  })
})
