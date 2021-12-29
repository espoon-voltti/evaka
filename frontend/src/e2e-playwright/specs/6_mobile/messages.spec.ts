// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import MobileChildPage from 'e2e-playwright/pages/mobile/child-page'
import MobileListPage from 'e2e-playwright/pages/mobile/list-page'
import PinLoginPage from 'e2e-playwright/pages/mobile/pin-login-page'
import { Page } from 'e2e-playwright/utils/page'
import { pairMobileDevice } from 'e2e-playwright/utils/mobile'
import config from 'e2e-test-common/config'
import {
  insertGuardianFixtures,
  resetDatabase,
  upsertMessageAccounts
} from 'e2e-test-common/dev-api'
import { Daycare, PersonDetail } from 'e2e-test-common/dev-api/types'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import {
  DaycareGroupBuilder,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import MobileMessageEditorPage from '../../pages/mobile/message-editor'
import MobileMessagesPage from '../../pages/mobile/messages'
import ThreadViewPage from '../../pages/mobile/thread-view'
import { waitUntilEqual } from '../../utils'
import { employeeLogin, enduserLogin } from '../../utils/user'
import MobileNav from '../../pages/mobile/mobile-nav'
import UnreadMobileMessagesPage from '../../pages/mobile/unread-message-counts'
import ChildInformationPage from '../../pages/employee/child-information-page'

let page: Page
let fixtures: AreaAndPersonFixtures
let listPage: MobileListPage
let childPage: MobileChildPage
let citizenPage: Page
let messageEditorPage: MobileMessageEditorPage
let messagesPage: MobileMessagesPage
let threadView: ThreadViewPage
let pinLoginPage: PinLoginPage
let unreadMessageCountsPage: UnreadMobileMessagesPage
let nav: MobileNav

const daycareGroupId = uuidv4()
const daycareGroup2Id = uuidv4()
const daycareGroup3Id = uuidv4()

let daycareGroup: DaycareGroupBuilder
let daycareGroup2: DaycareGroupBuilder
let daycareGroup3: DaycareGroupBuilder
let child: PersonDetail
let child2: PersonDetail

let unit: Daycare

const empFirstName = 'Yrjö'
const empLastName = 'Yksikkö'
const employeeName = `${empLastName} ${empFirstName}`

const staffFirstName = 'Sari'
const staffLastName = 'Staff'
const staffName = `${staffLastName} ${staffFirstName}`

const staff2FirstName = 'Mari'
const staff2LastName = 'Staff'
const staff2Name = `${staff2LastName} ${staff2FirstName}`

const pin = '2580'

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  child = fixtures.enduserChildFixtureJari
  child2 = fixtures.enduserChildFixtureKaarina
  unit = fixtures.daycareFixture

  daycareGroup = await Fixture.daycareGroup()
    .with({ id: daycareGroupId, daycareId: unit.id })
    .save()

  daycareGroup2 = await Fixture.daycareGroup()
    .with({ id: daycareGroup2Id, daycareId: unit.id })
    .save()

  daycareGroup3 = await Fixture.daycareGroup()
    .with({ id: daycareGroup3Id, daycareId: unit.id })
    .save()

  const employee = await Fixture.employee()
    .with({
      firstName: empFirstName,
      lastName: empLastName,
      email: 'yy@example.com',
      roles: []
    })
    .withDaycareAcl(unit.id, 'UNIT_SUPERVISOR')
    .save()

  const staff = await Fixture.employee()
    .with({
      firstName: staffFirstName,
      lastName: staffLastName,
      email: 'zz@example.com',
      roles: []
    })
    .withDaycareAcl(unit.id, 'STAFF')
    .withGroupAcl(daycareGroup.data.id)
    .withGroupAcl(daycareGroup2.data.id)
    .withGroupAcl(daycareGroup3.data.id)
    .save()

  const staff2 = await Fixture.employee()
    .with({
      firstName: staff2FirstName,
      lastName: staff2LastName,
      email: 'aa@example.com',
      roles: []
    })
    .withDaycareAcl(unit.id, 'STAFF')
    .withGroupAcl(daycareGroup.data.id)
    .withGroupAcl(daycareGroup2.data.id)
    .withGroupAcl(daycareGroup3.data.id)
    .save()

  await Fixture.employeePin().with({ userId: employee.data.id, pin }).save()
  await Fixture.employeePin().with({ userId: staff.data.id, pin }).save()
  await Fixture.employeePin().with({ userId: staff2.data.id, pin }).save()

  const placementFixture = await Fixture.placement()
    .with({ childId: child.id, unitId: unit.id })
    .save()
  await Fixture.groupPlacement()
    .withGroup(daycareGroup)
    .withPlacement(placementFixture)
    .save()

  const placement2Fixture = await Fixture.placement()
    .with({ childId: child2.id, unitId: unit.id })
    .save()
  await Fixture.groupPlacement()
    .withGroup(daycareGroup2)
    .withPlacement(placement2Fixture)
    .save()

  await upsertMessageAccounts()
  await insertGuardianFixtures([
    {
      childId: child.id,
      guardianId: fixtures.enduserGuardianFixture.id
    },
    {
      childId: child2.id,
      guardianId: fixtures.enduserGuardianFixture.id
    }
  ])

  page = await Page.open()
  listPage = new MobileListPage(page)
  childPage = new MobileChildPage(page)
  unreadMessageCountsPage = new UnreadMobileMessagesPage(page)
  pinLoginPage = new PinLoginPage(page)
  messageEditorPage = new MobileMessageEditorPage(page)
  messagesPage = new MobileMessagesPage(page)
  threadView = new ThreadViewPage(page)
  nav = new MobileNav(page)

  const mobileSignupUrl = await pairMobileDevice(unit.id)
  await page.goto(mobileSignupUrl)
})

async function initCitizenPage() {
  citizenPage = await Page.open()
  await enduserLogin(citizenPage)
}

describe('Message editor in child page', () => {
  test('Employee can open editor and send message', async () => {
    await listPage.selectChild(child.id)
    await childPage.openMessageEditor()
    await pinLoginPage.login(employeeName, pin)
    await messageEditorPage.draftNewMessage('Foo', 'Bar')
    await messageEditorPage.sendEditedMessage()
    await childPage.waitUntilLoaded()
  })
})

describe('Child message thread', () => {
  test('Employee sees unread counts and pin login button', async () => {
    await initCitizenPage()
    await citizenSendsMessageToGroup()
    await userSeesNewMessageIndicatorAndClicks()

    await unreadMessageCountsPage.groupLinksExist()
    await unreadMessageCountsPage.pinButtonExists()
  })

  test('Employee navigates using login button and sees messages', async () => {
    await initCitizenPage()
    await citizenSendsMessageToGroup()
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPage()
    await messagesPage.messagesExist()
  })

  test('Employee navigates using group link and sees messages', async () => {
    await initCitizenPage()
    await citizenSendsMessageToGroup()
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPageThroughGroup()
    await messagesPage.messagesExist()
  })

  test('Employee replies as a group to message sent to group', async () => {
    await initCitizenPage()
    await citizenSendsMessageToGroup()
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPage()

    await nav.selectGroup(daycareGroupId)

    await messagesPage.openFirstThread()
    await waitUntilEqual(() => threadView.countMessages(), 1)
    const replyContent = 'Testivastauksen sisältö'
    await threadView.replyThread(replyContent)
    await waitUntilEqual(() => threadView.countMessages(), 2)
    await waitUntilEqual(() => threadView.getMessageContent(1), replyContent)
    await waitUntilEqual(
      () => threadView.getMessageSender(1),
      `${unit.name} - ${daycareGroup.data.name}`
    )
  })

  test('Employee does not see messages to personal account', async () => {
    await initCitizenPage()
    await citizenSendsPersonalMessageToEmployee()
    await employeeLoginsToMessagesPage()
    await messagesPage.messagesDontExist()
  })

  test('Message button goes to unread messages if user has no pin session', async () => {
    await listPage.gotoMessages()
    await unreadMessageCountsPage.groupLinksExist()
    await unreadMessageCountsPage.pinButtonExists()
  })

  test('Message button goes to messages if user has pin session', async () => {
    await employeeLoginsToMessagesPage()
    await page.goto(config.mobileUrl)
    await listPage.gotoMessages()
    await messagesPage.messagesContainer.waitUntilVisible()
  })

  test("Staff sees citizen's message for group", async () => {
    await initCitizenPage()
    await citizenSendsMessageToGroup()
    await userSeesNewMessagesIndicator()
    await staffLoginsToMessagesPage()

    await messagesPage.messagesExist()
    await messagesPage.openFirstThread()

    await waitUntilEqual(() => threadView.countMessages(), 1)
    await waitUntilEqual(
      () => threadView.getMessageContent(0),
      'Testiviestin sisältö'
    )
  })

  test('Supervisor navigates through group message boxes', async () => {
    await initCitizenPage()
    await citizenSendsMessageToGroup()
    await citizenSendsMessageToGroup2()
    await userSeesNewMessagesIndicator()
    await employeeLoginsToMessagesPage()

    await nav.selectGroup(daycareGroup2.data.id)
    await messagesPage.messagesExist()
    await waitUntilEqual(() => messagesPage.getThreadTitle(0), 'Hei ryhmä 2')

    await nav.selectGroup(daycareGroup.data.id)
    await messagesPage.messagesExist()
    await waitUntilEqual(() => messagesPage.getThreadTitle(0), 'Otsikko')

    await nav.selectGroup(daycareGroup3.data.id)
    await messagesPage.messagesDontExist()
  })

  test('Staff without group access sees info that no accounts were found', async () => {
    await staff2LoginsToMessagesPage()
    await messagesPage.noAccountInfo.waitUntilVisible()
  })

  test('Employee sees info while trying to send message to child whose guardians are blocked', async () => {
    // Add child's guardians to block list
    const adminPage = await Page.open()
    await employeeLogin(adminPage, 'ADMIN')
    await adminPage.goto(`${config.employeeUrl}/child-information/${child.id}`)
    const childInformationPage = new ChildInformationPage(adminPage)
    await childInformationPage.addParentToBlockList(
      fixtures.enduserGuardianFixture.id
    )
    await childInformationPage.addParentToBlockList(
      fixtures.enduserChildJariOtherGuardianFixture.id
    )

    await listPage.selectChild(child.id)
    await childPage.openMessageEditor()
    await pinLoginPage.login(employeeName, pin)
    await messageEditorPage.noReceiversInfo.waitUntilVisible()
  })
})

async function citizenSendsMessageToGroup() {
  await citizenPage.goto(config.enduserMessagesUrl)
  const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
  const title = 'Otsikko'
  const content = 'Testiviestin sisältö'
  const receivers = [daycareGroup.data.name + ' (Henkilökunta)']
  await citizenMessagesPage.sendNewMessage(title, content, receivers)
}

async function citizenSendsMessageToGroup2() {
  await citizenPage.goto(config.enduserMessagesUrl)
  const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
  const title = 'Hei ryhmä 2'
  const content = 'Testiviestin sisältö'
  const receivers = [daycareGroup2.data.name + ' (Henkilökunta)']
  await citizenMessagesPage.sendNewMessage(title, content, receivers)
}

async function userSeesNewMessagesIndicator() {
  await page.goto(config.mobileUrl)
  await listPage.unreadMessagesIndicator.waitUntilVisible()
}

async function userSeesNewMessageIndicatorAndClicks() {
  await userSeesNewMessagesIndicator()
  await listPage.gotoMessages()
}

async function staffLoginsToMessagesPage() {
  await listPage.gotoMessages()
  await unreadMessageCountsPage.pinLoginButton.click()
  await pinLoginPage.login(staffName, pin)
}

async function staff2LoginsToMessagesPage() {
  await listPage.gotoMessages()
  await unreadMessageCountsPage.pinLoginButton.click()
  await pinLoginPage.login(staff2Name, pin)
}

async function employeeNavigatesToMessagesSelectingLogin() {
  await unreadMessageCountsPage.pinLoginButton.click()
  await pinLoginPage.login(employeeName, pin)
}

async function employeeNavigatesToMessagesSelectingGroup() {
  const linkToGroup = unreadMessageCountsPage.linkToGroup(daycareGroupId)
  await linkToGroup.waitUntilVisible()
  await linkToGroup.click()
  await pinLoginPage.login(employeeName, pin)
}

async function employeeLoginsToMessagesPage() {
  await listPage.gotoMessages()
  await employeeNavigatesToMessagesSelectingLogin()
}

async function employeeLoginsToMessagesPageThroughGroup() {
  await listPage.gotoMessages()
  await employeeNavigatesToMessagesSelectingGroup()
}

async function citizenSendsPersonalMessageToEmployee() {
  const title = 'Otsikko'
  const content = 'Testiviestin sisältö'
  const receivers = [`${empLastName} ${empFirstName}`]
  await citizenPage.goto(config.enduserMessagesUrl)
  const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
  await citizenMessagesPage.sendNewMessage(title, content, receivers)
  await citizenPage.close()
}
